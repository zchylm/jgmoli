package com.aicyber.jgmoli.admin.service;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
import com.aicyber.jgmoli.admin.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminService {
    private static final UUID MELBOURNE_LOCATION = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Set<String> PRODUCT_STATUSES = Set.of("DRAFT", "ACTIVE", "ARCHIVED");
    private static final Set<String> FULFILMENT_STATUSES = Set.of("UNFULFILLED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
    private final AdminRepository repository;
    private final ObjectMapper objectMapper;

    public AdminService(AdminRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminDtos.Overview overview() {
        return repository.overview();
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminDtos.OrderSummary> orders(String query, String status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return repository.orders(clean(query), normaliseOptional(status), safePage * safeSize, safeSize);
    }

    @Transactional(readOnly = true)
    public AdminDtos.OrderDetail order(String reference) {
        return repository.order(reference).orElseThrow(() -> new IllegalArgumentException("We could not find that order."));
    }

    @Transactional
    public AdminDtos.OrderDetail updateFulfilment(UUID adminId, String reference, AdminDtos.FulfillmentUpdate request) {
        AdminDtos.OrderDetail before = order(reference);
        String next = normaliseRequired(request == null ? null : request.status(), "Choose a fulfilment status.");
        if (!FULFILMENT_STATUSES.contains(next)) throw new IllegalArgumentException("Choose a valid fulfilment status.");
        if (!"PAID".equals(before.summary().status()) && !"CANCELLED".equals(next)) {
            throw new IllegalStateException("Only paid orders can move into fulfilment.");
        }
        ensureTransition(before.summary().fulfillmentStatus(), next);
        String carrier = clean(request.carrier());
        String tracking = clean(request.trackingNumber());
        if ("SHIPPED".equals(next) && (carrier.isBlank() || tracking.isBlank())) {
            throw new IllegalArgumentException("Add the carrier and tracking number before marking this order shipped.");
        }
        repository.jdbc().update("""
                UPDATE sales_orders
                SET fulfillment_status = ?, carrier = NULLIF(?, ''), tracking_number = NULLIF(?, ''),
                    shipped_at = CASE WHEN ? = 'SHIPPED' THEN COALESCE(shipped_at, CURRENT_TIMESTAMP) ELSE shipped_at END,
                    delivered_at = CASE WHEN ? = 'DELIVERED' THEN COALESCE(delivered_at, CURRENT_TIMESTAMP) ELSE delivered_at END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE order_reference = ?
                """, next, carrier, tracking, next, next, reference);
        AdminDtos.OrderDetail after = order(reference);
        audit(adminId, "ORDER_FULFILMENT_UPDATED", "SALES_ORDER", reference, before, after);
        return after;
    }

    private void ensureTransition(String current, String next) {
        if (current.equals(next)) return;
        boolean allowed = switch (current) {
            case "UNFULFILLED" -> Set.of("PROCESSING", "CANCELLED").contains(next);
            case "PROCESSING" -> Set.of("SHIPPED", "CANCELLED").contains(next);
            case "SHIPPED" -> "DELIVERED".equals(next);
            default -> false;
        };
        if (!allowed) throw new IllegalStateException("That fulfilment status cannot follow " + current.toLowerCase() + ".");
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminDtos.Product> products() {
        return repository.products();
    }

    @Transactional
    public AdminDtos.Product updateProduct(UUID adminId, UUID variantId, AdminDtos.ProductUpdate request) {
        AdminDtos.Product before = repository.product(variantId)
                .orElseThrow(() -> new IllegalArgumentException("We could not find that product."));
        if (request == null) throw new IllegalArgumentException("Choose a product change.");
        long price = request.priceCents() == null ? before.priceCents() : request.priceCents();
        if (price < 0) throw new IllegalArgumentException("Enter a valid GST-inclusive price.");
        String status = request.status() == null ? before.status() : request.status().trim().toUpperCase();
        if (!PRODUCT_STATUSES.contains(status)) throw new IllegalArgumentException("Choose a valid product status.");
        boolean active = request.active() == null ? before.active() : request.active();
        repository.jdbc().update("UPDATE product_variants SET price_cents = ?, active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                price, active, variantId);
        repository.jdbc().update("UPDATE products SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                status, before.productId());
        AdminDtos.Product after = repository.product(variantId).orElseThrow();
        audit(adminId, "PRODUCT_UPDATED", "PRODUCT_VARIANT", variantId.toString(), before, after);
        return after;
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminDtos.Inventory> inventory() {
        return repository.inventory();
    }

    @Transactional(readOnly = true)
    public java.util.List<AdminDtos.Movement> movements() {
        return repository.movements();
    }

    @Transactional
    public AdminDtos.Inventory createInventoryItem(UUID adminId, AdminDtos.InventoryItemCreate request) {
        if (request == null) throw new IllegalArgumentException("Enter the warehouse item details.");
        String sku = normaliseRequired(request.sku(), "Enter a unique SKU.");
        if (sku.length() > 80 || !sku.matches("[A-Z0-9][A-Z0-9._-]*")) {
            throw new IllegalArgumentException("Use letters, numbers, dots, dashes or underscores for the SKU.");
        }
        String brand = requiredText(request.brand(), "Enter the brand.", 100);
        String name = requiredText(request.productName(), "Enter the product name.", 180);
        String categoryCode = normaliseRequired(request.categoryCode(), "Choose a store category.").toLowerCase(Locale.ROOT);
        String type = requiredText(request.productType(), "Enter the product type.", 80);
        if (request.initialQuantity() < 0 || request.reorderLevel() < 0) {
            throw new IllegalArgumentException("Enter valid starting stock and reorder quantities.");
        }
        String reason = requiredText(request.reason(), "Add a reason for the opening stock balance.", 240);
        Integer duplicate = repository.jdbc().queryForObject("""
                SELECT (SELECT COUNT(*) FROM inventory_items WHERE sku = ?)
                     + (SELECT COUNT(*) FROM product_variants WHERE sku = ?)
                """, Integer.class, sku, sku);
        if (duplicate != null && duplicate > 0) throw new IllegalStateException("That SKU already exists in inventory.");

        Map<String, Object> category = repository.jdbc().query("""
                SELECT id, code FROM product_categories WHERE code = ?
                """, rs -> rs.next() ? Map.of("id", rs.getObject("id", UUID.class), "code", rs.getString("code")) : null,
                categoryCode);
        if (category == null) throw new IllegalArgumentException("Choose a valid store category.");

        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        repository.jdbc().update("""
                INSERT INTO products
                    (id, category_id, slug, brand, name, subtype, short_description,
                     image_key, status, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', 999)
                """, productId, category.get("id"), slug(brand, name, sku), brand, name, type,
                "Storefront details are being prepared.", placeholderImage(categoryCode));
        repository.jdbc().update("""
                INSERT INTO product_variants
                    (id, product_id, sku, name, price_cents, active)
                VALUES (?, ?, ?, 'Standard', 0, FALSE)
                """, variantId, productId, sku);
        repository.jdbc().update("""
                INSERT INTO inventory_items (id, catalog_variant_id, sku, brand, name, item_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """, itemId, variantId, sku, brand, name, type);
        repository.jdbc().update("""
                INSERT INTO inventory_balances
                    (variant_id, item_id, location_id, on_hand, reorder_level)
                VALUES (NULL, ?, ?, ?, ?)
                """, itemId, MELBOURNE_LOCATION, request.initialQuantity(), request.reorderLevel());
        if (request.initialQuantity() > 0) {
            repository.jdbc().update("""
                    INSERT INTO inventory_movements
                        (id, variant_id, item_id, location_id, movement_type, on_hand_delta, reason, performed_by)
                    VALUES (?, NULL, ?, ?, 'RECEIPT', ?, ?, ?)
                    """, UUID.randomUUID(), itemId, MELBOURNE_LOCATION, request.initialQuantity(), reason, adminId);
        }
        AdminDtos.Inventory created = repository.inventory(itemId).orElseThrow();
        audit(adminId, "CATALOG_AND_INVENTORY_ITEM_CREATED", "INVENTORY_ITEM", itemId.toString(), null, created);
        return created;
    }

    @Transactional
    public AdminDtos.Inventory adjustInventory(
            UUID adminId,
            UUID itemId,
            String idempotencyKey,
            AdminDtos.InventoryAdjustment request
    ) {
        String key = normaliseRequired(idempotencyKey, "Start the inventory change again.");
        if (key.length() > 120) throw new IllegalArgumentException("Start the inventory change again.");
        if (request == null) throw new IllegalArgumentException("Enter an inventory change.");
        String type = normaliseRequired(request.movementType(), "Choose receipt or adjustment.");
        if (!Set.of("RECEIPT", "ADJUSTMENT").contains(type)) {
            throw new IllegalArgumentException("Choose receipt or adjustment.");
        }
        if (request.quantityDelta() == 0 || ("RECEIPT".equals(type) && request.quantityDelta() < 1)) {
            throw new IllegalArgumentException("Enter a valid inventory quantity.");
        }
        String reason = normaliseRequired(request.reason(), "Add a reason for this inventory change.");
        if (reason.length() > 240) throw new IllegalArgumentException("Keep the inventory reason under 240 characters.");

        Integer duplicate = repository.jdbc().queryForObject("""
                SELECT COUNT(*) FROM inventory_movements WHERE performed_by = ? AND idempotency_key = ?
                """, Integer.class, adminId, key);
        if (duplicate != null && duplicate > 0) {
            return repository.inventory(itemId).orElseThrow(() -> new IllegalArgumentException("We could not find that inventory item."));
        }

        Map<String, Object> locked = repository.jdbc().queryForMap("""
                SELECT on_hand, reserved, reorder_level FROM inventory_balances
                WHERE item_id = ? AND location_id = ? FOR UPDATE
                """, itemId, MELBOURNE_LOCATION);
        int currentOnHand = ((Number) locked.get("on_hand")).intValue();
        int reserved = ((Number) locked.get("reserved")).intValue();
        int nextOnHand = Math.addExact(currentOnHand, request.quantityDelta());
        if (nextOnHand < reserved) throw new IllegalStateException("On-hand stock cannot be lower than reserved stock.");
        int reorderLevel = request.reorderLevel() == null
                ? ((Number) locked.get("reorder_level")).intValue()
                : request.reorderLevel();
        if (reorderLevel < 0) throw new IllegalArgumentException("Enter a valid reorder level.");

        AdminDtos.Inventory before = repository.inventory(itemId)
                .orElseThrow(() -> new IllegalArgumentException("We could not find that inventory item."));
        repository.jdbc().update("""
                UPDATE inventory_balances
                SET on_hand = ?, reorder_level = ?, version = version + 1, updated_at = CURRENT_TIMESTAMP
                WHERE item_id = ? AND location_id = ?
                """, nextOnHand, reorderLevel, itemId, MELBOURNE_LOCATION);
        repository.jdbc().update("""
                INSERT INTO inventory_movements
                    (id, variant_id, item_id, location_id, movement_type, on_hand_delta, reason,
                     idempotency_key, performed_by)
                VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), itemId, MELBOURNE_LOCATION, type, request.quantityDelta(), reason, key, adminId);
        AdminDtos.Inventory after = repository.inventory(itemId).orElseThrow();
        audit(adminId, "INVENTORY_" + type, "INVENTORY_ITEM", itemId.toString(), before, after);
        return after;
    }

    private void audit(UUID adminId, String action, String entityType, String entityId, Object before, Object after) {
        repository.jdbc().update("""
                INSERT INTO admin_audit_events
                    (id, admin_user_id, action, entity_type, entity_id, before_state, after_state)
                VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb))
                """, UUID.randomUUID(), adminId, action, entityType, entityId, json(before), json(after));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("The admin change could not be recorded.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normaliseOptional(String value) {
        return clean(value).toUpperCase();
    }

    private String normaliseRequired(String value, String message) {
        String normalised = normaliseOptional(value);
        if (normalised.isBlank()) throw new IllegalArgumentException(message);
        return normalised;
    }

    private String requiredText(String value, String message, int maxLength) {
        String normalised = clean(value);
        if (normalised.isBlank() || normalised.length() > maxLength) throw new IllegalArgumentException(message);
        return normalised;
    }

    private String slug(String brand, String name, String sku) {
        String value = (brand + "-" + name + "-" + sku).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return value.substring(0, Math.min(value.length(), 160));
    }

    private String placeholderImage(String categoryCode) {
        return switch (categoryCode) {
            case "displays" -> "display-high-refresh";
            case "controls" -> "control-controller";
            case "audio" -> "audio-headset";
            case "sim" -> "sim-wheel";
            case "furniture" -> "furniture-desk";
            default -> throw new IllegalArgumentException("Choose a valid store category.");
        };
    }
}
