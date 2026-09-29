package com.aicyber.jgmoli.payment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        String paymentReference,
        String orderReference,
        String orderStatus,
        String paymentStatus,
        long amountCents,
        String currency,
        OffsetDateTime paidAt
) {
}
