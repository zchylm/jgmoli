package com.aicyber.jgmoli.checkout.dto;

import java.util.UUID;

public record OrderLineResponse(
        UUID variantId,
        String sku,
        String brand,
        String productName,
        String variantName,
        int quantity,
        long unitPriceCents,
        long gstCents,
        long lineTotalCents
) {
}
