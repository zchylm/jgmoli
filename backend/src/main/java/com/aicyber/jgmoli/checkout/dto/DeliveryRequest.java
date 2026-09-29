package com.aicyber.jgmoli.checkout.dto;

public record DeliveryRequest(
        String recipientName,
        String phone,
        String addressLine1,
        String addressLine2,
        String suburb,
        String state,
        String postcode,
        String countryCode
) {
}
