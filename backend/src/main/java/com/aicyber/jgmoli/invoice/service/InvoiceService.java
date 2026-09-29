package com.aicyber.jgmoli.invoice.service;

import com.aicyber.jgmoli.invoice.config.InvoiceBusinessDetails;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import com.aicyber.jgmoli.invoice.repository.InvoiceRepository;
import com.aicyber.jgmoli.email.service.TransactionalEmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");
    private final InvoiceRepository invoiceRepository;
    private final InvoiceBusinessDetails businessDetails;
    private final TransactionalEmailService emailService;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceBusinessDetails businessDetails,
                          TransactionalEmailService emailService) {
        this.invoiceRepository = invoiceRepository;
        this.businessDetails = businessDetails;
        this.emailService = emailService;
    }

    @Transactional
    public InvoiceResponse issueForSuccessfulPayment(UUID userId, String orderReference) {
        var existing = invoiceRepository.findByOrder(userId, orderReference);
        if (existing.isPresent()) {
            emailService.queueInvoiceIssued(existing.get());
            return existing.get();
        }

        var source = invoiceRepository.sourceForIssue(userId, orderReference)
                .orElseThrow(() -> new IllegalStateException("A tax invoice requires a confirmed payment."));
        if (!"PAID".equals(source.orderStatus()) || !"SUCCEEDED".equals(source.paymentStatus())) {
            throw new IllegalStateException("A tax invoice requires a confirmed payment.");
        }
        if (source.totalCents() != source.amountPaidCents()) {
            throw new IllegalStateException("The confirmed payment does not match the order total.");
        }
        if (!"AUD".equals(source.currency())) {
            throw new IllegalStateException("Tax invoices are currently issued in AUD only.");
        }

        OffsetDateTime issuedAt = OffsetDateTime.now(MELBOURNE);
        String invoiceNumber = "JGM-INV-%d-%06d".formatted(
                issuedAt.getYear(), invoiceRepository.nextInvoiceSequence());
        UUID invoiceId = UUID.randomUUID();
        invoiceRepository.create(invoiceId, invoiceNumber, source, businessDetails, issuedAt,
                invoiceRepository.sourceLines(source.orderId()));
        InvoiceResponse invoice = invoiceRepository.findByOrder(userId, orderReference).orElseThrow();
        emailService.queueInvoiceIssued(invoice);
        return invoice;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse invoiceForOrder(UUID userId, String orderReference) {
        return invoiceRepository.findByOrder(userId, orderReference)
                .orElseThrow(() -> new IllegalArgumentException("We could not find a tax invoice for that order."));
    }
}
