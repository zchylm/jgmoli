package com.aicyber.jgmoli.checkout.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderReference,
        String source,
        String status,
        String currency,
        long subtotalExGstCents,
        long gstCents,
        long deliveryCents,
        long totalCents,
        String customerEmail,
        DeliveryRequest delivery,
        List<OrderLineResponse> items,
        OffsetDateTime createdAt,
        OffsetDateTime paidAt
) {
}
