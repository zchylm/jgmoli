package com.aicyber.jgmoli.invoice.dto;

public record InvoiceLineResponse(
        int lineNumber,
        String sku,
        String description,
        int quantity,
        long unitPriceExGstCents,
        long gstCents,
        long lineTotalIncGstCents,
        boolean taxable
) {
}
