package com.aicyber.jgmoli.invoice.model;

import com.aicyber.jgmoli.invoice.dto.InvoiceAddressResponse;

import java.util.UUID;

public record InvoiceSource(
        UUID orderId,
        UUID paymentId,
        String orderReference,
        String paymentReference,
        String orderStatus,
        String paymentStatus,
        String buyerName,
        String buyerEmail,
        InvoiceAddressResponse buyerAddress,
        String currency,
        long subtotalExGstCents,
        long gstCents,
        long deliveryCents,
        long totalCents,
        long amountPaidCents
) {
}
