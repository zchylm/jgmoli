package com.aicyber.jgmoli.email.service;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
import com.aicyber.jgmoli.auth.model.User;
import com.aicyber.jgmoli.checkout.dto.OrderResponse;
import com.aicyber.jgmoli.email.model.EmailDraft;
import com.aicyber.jgmoli.email.template.EmailContent;
import com.aicyber.jgmoli.email.template.EmailTemplateFactory;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionalEmailService {
    private final EmailOutboxService outbox;
    private final EmailTemplateFactory templates;
    private final String storeUrl;

    public TransactionalEmailService(
            EmailOutboxService outbox,
            EmailTemplateFactory templates,
            @Value("${jgmoli.email.store-url:http://localhost:5173}") String storeUrl
    ) {
        this.outbox = outbox;
        this.templates = templates;
        this.storeUrl = stripTrailingSlash(storeUrl);
    }

    public void queueVerification(User user, UUID tokenId, String token) {
        EmailContent content = templates.verification(user.displayName(), storeUrl + "/?verifyEmail=" + token);
        enqueue("EMAIL_VERIFICATION", user.email(), user.displayName(), content,
                "USER", user.id(), "email-verification:" + tokenId);
    }

    public void queuePasswordReset(User user, UUID tokenId, String token) {
        EmailContent content = templates.passwordReset(user.displayName(), storeUrl + "/?resetPassword=" + token);
        enqueue("PASSWORD_RESET", user.email(), user.displayName(), content,
                "USER", user.id(), "password-reset:" + tokenId);
    }

    public void queueOrderCreated(OrderResponse order) {
        EmailContent content = templates.orderCreated(order, storeUrl);
        enqueue("ORDER_CREATED", order.customerEmail(), order.delivery().recipientName(), content,
                "ORDER", order.id(), "order-created:" + order.id());
    }

    public void queueInvoiceIssued(InvoiceResponse invoice) {
        EmailContent content = templates.invoiceIssued(invoice, storeUrl);
        enqueue("INVOICE_ISSUED", invoice.buyerEmail(), invoice.buyerName(), content,
                "INVOICE", invoice.id(), "invoice-issued:" + invoice.id());
    }

    public void queueFulfilmentUpdated(AdminDtos.OrderDetail order) {
        if (!java.util.Set.of("SHIPPED", "DELIVERED", "CANCELLED")
                .contains(order.summary().fulfillmentStatus())) return;
        EmailContent content = templates.fulfilmentUpdated(order, storeUrl);
        enqueue("ORDER_" + order.summary().fulfillmentStatus(), order.summary().customerEmail(),
                order.summary().customerName(), content, "ORDER", order.summary().id(),
                "order-fulfilment:" + order.summary().id() + ":" + order.summary().fulfillmentStatus());
    }

    private void enqueue(String type, String recipient, String name, EmailContent content,
                         String aggregateType, UUID aggregateId, String idempotencyKey) {
        outbox.enqueue(new EmailDraft(type, recipient, name, content.subject(), content.textBody(), content.htmlBody(),
                aggregateType, aggregateId, idempotencyKey));
    }

    private String stripTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
