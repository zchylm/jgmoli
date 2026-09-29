package com.aicyber.jgmoli.catalog.dto;

import java.util.UUID;

public record CatalogProductResponse(
        UUID id,
        UUID variantId,
        String slug,
        String sku,
        String category,
        String categoryName,
        String subtype,
        String brand,
        String name,
        String description,
        String imageKey,
        int priceCents,
        String currency,
        boolean priceIncludesGst
) {
}
