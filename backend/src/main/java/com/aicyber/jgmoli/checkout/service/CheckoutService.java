package com.aicyber.jgmoli.checkout.service;

import com.aicyber.jgmoli.auth.repository.UserRepository;
import com.aicyber.jgmoli.checkout.dto.CheckoutLineRequest;
import com.aicyber.jgmoli.checkout.dto.CreateOrderRequest;
import com.aicyber.jgmoli.checkout.dto.DeliveryRequest;
import com.aicyber.jgmoli.checkout.dto.OrderResponse;
import com.aicyber.jgmoli.checkout.model.CheckoutVariant;
import com.aicyber.jgmoli.checkout.repository.CheckoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CheckoutService {

    private static final Set<String> AUSTRALIAN_STATES = Set.of("ACT", "NSW", "NT", "QLD", "SA", "TAS", "VIC", "WA");
    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");
    private final CheckoutRepository checkoutRepository;
    private final UserRepository userRepository;

    public CheckoutService(CheckoutRepository checkoutRepository, UserRepository userRepository) {
        this.checkoutRepository = checkoutRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, String idempotencyKey, CreateOrderRequest request) {
        String key = requireText(idempotencyKey, "Start checkout again and try once more.", 120);
        String source = normaliseSource(request == null ? null : request.source());
        DeliveryRequest delivery = normaliseDelivery(request == null ? null : request.delivery());
        Map<UUID, Integer> quantities = normaliseItems(request == null ? null : request.items());
        String fingerprint = fingerprint(source, delivery, quantities);

        var existing = checkoutRepository.findByIdempotencyKey(userId, key);
        if (existing.isPresent()) {
            if (!existing.get().requestFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("This checkout request changed. Return to your cart and try again.");
            }
            return requireOrder(userId, existing.get().orderReference());
        }

        var user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("Log in to continue."));
        List<CheckoutVariant> variants = checkoutRepository.findActiveVariants(quantities.keySet().stream().toList());
        if (variants.size() != quantities.size()) {
            throw new IllegalArgumentException("One or more products are no longer available. Review your selection and try again.");
        }
        String currency = variants.get(0).currency();
        if (variants.stream().anyMatch(variant -> !currency.equals(variant.currency()))) {
            throw new IllegalArgumentException("The selected products cannot be checked out together.");
        }

        List<PricedLine> pricedLines = variants.stream()
                .sorted(Comparator.comparing(CheckoutVariant::sku))
                .map(variant -> price(variant, quantities.get(variant.variantId())))
                .toList();
        long subtotalExGst = pricedLines.stream().mapToLong(PricedLine::subtotalExGstCents).sum();
        long gst = pricedLines.stream().mapToLong(PricedLine::gstCents).sum();
        long total = pricedLines.stream().mapToLong(PricedLine::lineTotalCents).sum();
        long sequence = checkoutRepository.nextOrderSequence();
        String orderReference = "JGM-%s-%06d".formatted(
                LocalDate.now(MELBOURNE).format(DateTimeFormatter.BASIC_ISO_DATE), sequence);
        UUID orderId = UUID.randomUUID();

        boolean created = checkoutRepository.createOrder(orderId, orderReference, userId, source, currency,
                subtotalExGst, gst, 0, total, user.email(), key, fingerprint);
        if (!created) {
            var concurrentOrder = checkoutRepository.findByIdempotencyKey(userId, key).orElseThrow();
            if (!concurrentOrder.requestFingerprint().equals(fingerprint)) {
                throw new IllegalStateException("This checkout request changed. Return to your cart and try again.");
            }
            return requireOrder(userId, concurrentOrder.orderReference());
        }
        for (int index = 0; index < pricedLines.size(); index++) {
            PricedLine line = pricedLines.get(index);
            checkoutRepository.createOrderLine(orderId, index + 1, line.variant(), line.quantity(),
                    line.unitPriceIncGstCents(), line.unitPriceExGstCents(), line.gstCents(), line.lineTotalCents());
        }
        checkoutRepository.createDelivery(orderId, delivery);
        return requireOrder(userId, orderReference);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, String orderReference) {
        return requireOrder(userId, orderReference);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders(UUID userId) {
        return checkoutRepository.findOrders(userId);
    }

    private OrderResponse requireOrder(UUID userId, String orderReference) {
        return checkoutRepository.findOrder(userId, orderReference)
                .orElseThrow(() -> new IllegalArgumentException("We could not find that order."));
    }

    private Map<UUID, Integer> normaliseItems(List<CheckoutLineRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one product before checkout.");
        }
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (CheckoutLineRequest item : items) {
            if (item == null || item.variantId() == null || item.quantity() < 1) {
                throw new IllegalArgumentException("Review product quantities and try again.");
            }
            int quantity = Math.addExact(quantities.getOrDefault(item.variantId(), 0), item.quantity());
            if (quantity > 99) {
                throw new IllegalArgumentException("A product quantity cannot be greater than 99.");
            }
            quantities.put(item.variantId(), quantity);
        }
        return quantities;
    }

    private DeliveryRequest normaliseDelivery(DeliveryRequest request) {
        if (request == null) throw new IllegalArgumentException("Enter a delivery address.");
        String state = requireText(request.state(), "Choose an Australian state or territory.", 10).toUpperCase(Locale.ROOT);
        if (!AUSTRALIAN_STATES.contains(state)) {
            throw new IllegalArgumentException("Choose an Australian state or territory.");
        }
        String postcode = requireText(request.postcode(), "Enter a four-digit postcode.", 10);
        if (!postcode.matches("\\d{4}")) throw new IllegalArgumentException("Enter a four-digit postcode.");
        String phone = requireText(request.phone(), "Enter a contact phone number.", 40);
        if (!phone.matches("[+0-9 ()-]{8,30}")) throw new IllegalArgumentException("Enter a valid contact phone number.");
        String country = request.countryCode() == null || request.countryCode().isBlank()
                ? "AU" : request.countryCode().trim().toUpperCase(Locale.ROOT);
        if (!"AU".equals(country)) throw new IllegalArgumentException("Delivery is currently available within Australia only.");
        return new DeliveryRequest(
                requireText(request.recipientName(), "Enter the recipient name.", 160),
                phone,
                requireText(request.addressLine1(), "Enter the delivery address.", 200),
                optionalText(request.addressLine2(), 200),
                requireText(request.suburb(), "Enter the suburb or city.", 120),
                state,
                postcode,
                country
        );
    }

    private String normaliseSource(String source) {
        if (source == null) throw new IllegalArgumentException("Choose where to start checkout.");
        String normalised = source.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("CART", "BUY_NOW").contains(normalised)) {
            throw new IllegalArgumentException("Choose where to start checkout.");
        }
        return normalised;
    }

    private PricedLine price(CheckoutVariant variant, int quantity) {
        long unitInc;
        long unitEx;
        long lineTotal;
        long subtotalEx;
        if (variant.priceIncludesGst()) {
            unitInc = variant.priceCents();
            unitEx = BigDecimal.valueOf(unitInc)
                    .divide(BigDecimal.ONE.add(variant.gstRate()), 0, RoundingMode.HALF_UP).longValueExact();
            lineTotal = Math.multiplyExact(unitInc, quantity);
            subtotalEx = BigDecimal.valueOf(lineTotal)
                    .divide(BigDecimal.ONE.add(variant.gstRate()), 0, RoundingMode.HALF_UP).longValueExact();
        } else {
            unitEx = variant.priceCents();
            unitInc = BigDecimal.valueOf(unitEx)
                    .multiply(BigDecimal.ONE.add(variant.gstRate())).setScale(0, RoundingMode.HALF_UP).longValueExact();
            subtotalEx = Math.multiplyExact(unitEx, quantity);
            lineTotal = BigDecimal.valueOf(subtotalEx)
                    .multiply(BigDecimal.ONE.add(variant.gstRate())).setScale(0, RoundingMode.HALF_UP).longValueExact();
        }
        return new PricedLine(variant, quantity, unitInc, unitEx, lineTotal - subtotalEx, subtotalEx, lineTotal);
    }

    private String fingerprint(String source, DeliveryRequest delivery, Map<UUID, Integer> quantities) {
        String itemText = quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .reduce((left, right) -> left + "," + right).orElse("");
        String value = String.join("|", source, itemText, delivery.recipientName(), delivery.phone(),
                delivery.addressLine1(), delivery.addressLine2() == null ? "" : delivery.addressLine2(),
                delivery.suburb(), delivery.state(), delivery.postcode(), delivery.countryCode());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Checkout is temporarily unavailable.");
        }
    }

    private String requireText(String value, String message, int maxLength) {
        String normalised = value == null ? "" : value.trim();
        if (normalised.isEmpty() || normalised.length() > maxLength) throw new IllegalArgumentException(message);
        return normalised;
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalised = value.trim();
        if (normalised.length() > maxLength) throw new IllegalArgumentException("Check the delivery address and try again.");
        return normalised;
    }

    private record PricedLine(
            CheckoutVariant variant,
            int quantity,
            long unitPriceIncGstCents,
            long unitPriceExGstCents,
            long gstCents,
            long subtotalExGstCents,
            long lineTotalCents
    ) {
    }
}
