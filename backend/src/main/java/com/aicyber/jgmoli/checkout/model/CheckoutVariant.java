package com.aicyber.jgmoli.checkout.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutVariant(
        UUID productId,
        UUID variantId,
        String sku,
        String brand,
        String productName,
        String variantName,
        long priceCents,
        String currency,
        BigDecimal gstRate,
        boolean priceIncludesGst
) {
}
