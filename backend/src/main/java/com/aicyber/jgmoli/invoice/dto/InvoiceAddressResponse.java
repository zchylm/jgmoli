package com.aicyber.jgmoli.invoice.dto;

public record InvoiceAddressResponse(
        String addressLine1,
        String addressLine2,
        String suburb,
        String state,
        String postcode,
        String countryCode
) {
}
