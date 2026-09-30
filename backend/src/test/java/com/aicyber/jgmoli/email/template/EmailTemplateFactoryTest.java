package com.aicyber.jgmoli.email.template;

import com.aicyber.jgmoli.invoice.dto.InvoiceAddressResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceLineResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateFactoryTest {
    private final EmailTemplateFactory templates = new EmailTemplateFactory();

    @Test
    void rendersARestrainedOrderConfirmationWithInvoiceDetails() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
                UUID.randomUUID(), "JGM-INV-2026-001032", "TAX_INVOICE", "ISSUED",
                "JGM-20260930-001038", "JGM-PAY-20260930-001037",
                "AI CYBER AUSTRALIA PTY LTD", "JG MOLI", "22 689 546 450",
                "205 Kensington Rd, West Melbourne VIC 3003, Australia",
                "support@jgmoli.com.au", "+61 436 365 016", "Jambo",
                "customer@example.com", new InvoiceAddressResponse(
                "23 MacKenzie Street", null, "Melbourne", "VIC", "3000", "AU"),
                "AUD", 118091, 11809, 0, 129900, 129900, OffsetDateTime.now(),
                List.of(new InvoiceLineResponse(1, "DEMO-AW3423DWF",
                        "Alienware AW3423DWF 34-inch QD-OLED Monitor", 1,
                        118091, 11809, 129900, true))
        );

        EmailContent content = templates.invoiceIssued(invoice, "https://jgmoli.com.au");

        assertTrue(content.subject().startsWith("Order confirmed JGM-20260930-001038"));
        assertTrue(content.htmlBody().contains("Your setup is confirmed."));
        assertTrue(content.htmlBody().contains("tax invoice is attached as a PDF"));
        assertTrue(content.htmlBody().contains("background:#fbfaf7"));
        assertFalse(content.htmlBody().contains("Your world is on its way."));
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target", "order-confirmation-email-preview.html"), content.htmlBody());
    }
}
