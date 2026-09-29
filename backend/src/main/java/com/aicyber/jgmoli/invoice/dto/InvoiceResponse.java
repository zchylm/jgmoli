package com.aicyber.jgmoli.invoice.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String invoiceNumber,
        String documentType,
        String status,
        String orderReference,
        String paymentReference,
        String sellerLegalName,
        String sellerTradingName,
        String sellerAbn,
        String sellerAddress,
        String sellerEmail,
        String sellerPhone,
        String buyerName,
        String buyerEmail,
        InvoiceAddressResponse buyerAddress,
        String currency,
        long subtotalExGstCents,
        long gstCents,
        long deliveryCents,
        long totalCents,
        long amountPaidCents,
        OffsetDateTime issuedAt,
        List<InvoiceLineResponse> lines
) {
}
