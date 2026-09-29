package com.aicyber.jgmoli.payment.service;

import com.aicyber.jgmoli.payment.dto.PaymentResponse;
import com.aicyber.jgmoli.payment.repository.PaymentRepository;
import com.aicyber.jgmoli.invoice.service.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class DemoPaymentService {

    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;

    public DemoPaymentService(PaymentRepository paymentRepository, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public PaymentResponse complete(UUID userId, String idempotencyKey, String orderReference) {
        String key = requireText(idempotencyKey, "Restart payment and try again.", 120);
        String reference = requireText(orderReference, "Choose an order to pay.", 40);
        var order = paymentRepository.lockOrder(userId, reference)
                .orElseThrow(() -> new IllegalArgumentException("We could not find that order."));
        var repeated = paymentRepository.findByIdempotencyKey(order.id(), key);
        if (repeated.isPresent()) {
            invoiceService.issueForSuccessfulPayment(userId, reference);
            return repeated.get();
        }
        var succeeded = paymentRepository.findSucceeded(order.id());
        if (succeeded.isPresent()) {
            invoiceService.issueForSuccessfulPayment(userId, reference);
            return succeeded.get();
        }
        if (!"AWAITING_PAYMENT".equals(order.status()) && !"PAYMENT_FAILED".equals(order.status())) {
            throw new IllegalStateException("This order is not ready for payment.");
        }
        long sequence = paymentRepository.nextPaymentSequence();
        String paymentReference = "JGM-PAY-%s-%06d".formatted(
                LocalDate.now(MELBOURNE).format(DateTimeFormatter.BASIC_ISO_DATE), sequence);
        PaymentResponse payment = paymentRepository.completeDemoPayment(order, paymentReference, key);
        invoiceService.issueForSuccessfulPayment(userId, reference);
        return payment;
    }

    private String requireText(String value, String message, int maxLength) {
        String normalised = value == null ? "" : value.trim();
        if (normalised.isEmpty() || normalised.length() > maxLength) throw new IllegalArgumentException(message);
        return normalised;
    }
}
