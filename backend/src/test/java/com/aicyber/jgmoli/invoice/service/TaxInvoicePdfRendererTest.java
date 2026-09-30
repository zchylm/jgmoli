package com.aicyber.jgmoli.invoice.service;

import com.aicyber.jgmoli.invoice.dto.InvoiceAddressResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceLineResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxInvoicePdfRendererTest {

    @Test
    void rendersAReadableTaxInvoice() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
                UUID.randomUUID(), "JGM-INV-2026-000001", "TAX_INVOICE", "ISSUED",
                "JGM-20260930-000001", "JGM-PAY-20260930-000001",
                "AI CYBER AUSTRALIA PTY LTD", "JG MOLI", "22 689 546 450",
                "205 Kensington Rd, West Melbourne VIC 3003, Australia",
                "support@jgmoli.com.au", "+61 436 365 016",
                "Jordan Player", "jordan@example.com",
                new InvoiceAddressResponse("10 Collins Street", "Level 2", "Melbourne", "VIC", "3000", "AU"),
                "AUD", 434912, 43488, 0, 478400, 478400,
                OffsetDateTime.parse("2026-09-30T10:00:00+10:00"),
                IntStream.rangeClosed(1, 16)
                        .mapToObj(number -> new InvoiceLineResponse(number, "DEMO-SKU-%02d".formatted(number),
                                "Gaming equipment item %02d".formatted(number), 1, 27182, 2718, 29900, true))
                        .toList()
        );

        byte[] pdf = new TaxInvoicePdfRenderer().render(invoice);
        Path sample = Path.of("target", "jgmoli-tax-invoice-sample.pdf");
        Files.write(sample, pdf);

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("TAX INVOICE"));
            assertTrue(text.contains("ABN 22 689 546 450"));
            assertTrue(text.contains("JGM-INV-2026-000001"));
            assertTrue(text.contains("Amount paid"));
            assertTrue(text.contains("$4,784.00"));
        }
    }
}
