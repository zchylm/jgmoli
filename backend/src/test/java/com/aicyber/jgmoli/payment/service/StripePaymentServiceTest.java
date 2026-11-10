package com.aicyber.jgmoli.payment.service;

import com.aicyber.jgmoli.invoice.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StripePaymentServiceTest {
    @Test
    void disabledAndLiveKeysCannotCreateCheckout() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        InvoiceService invoices = mock(InvoiceService.class);
        assertThrows(IllegalStateException.class, () -> new StripePaymentService(db, invoices, mock(TransactionTemplate.class), new StripePaymentSettings(false, "test", false, false, "sk_test_example", "whsec_example", "http://localhost:5173/")).create(UUID.randomUUID(), "order"));
        assertThrows(IllegalStateException.class, () -> new StripePaymentService(db, invoices, mock(TransactionTemplate.class), new StripePaymentSettings(true, "test", false, false, "sk_live_example", "whsec_example", "http://localhost:5173/")).create(UUID.randomUUID(), "order"));
        verifyNoInteractions(db, invoices);
    }

    @Test
    void invalidWebhookSignatureCannotChangeOrders() {
        JdbcTemplate db = mock(JdbcTemplate.class);
        InvoiceService invoices = mock(InvoiceService.class);
        var service = new StripePaymentService(db, invoices, mock(TransactionTemplate.class), new StripePaymentSettings(true, "test", false, false, "sk_test_example", "whsec_example", "http://localhost:5173/"));
        assertThrows(IllegalArgumentException.class, () -> service.webhook("{}", "t=1,v1=invalid"));
        verifyNoInteractions(db, invoices);
    }
}
