package com.aicyber.jgmoli.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record Overview(
            long ordersToday,
            long paidRevenueTodayCents,
            long awaitingPayment,
            long needsFulfilment,
            long lowStockProducts,
            List<OrderSummary> recentOrders
    ) {
    }

    public record OrderSummary(
            UUID id,
            String orderReference,
            String customerName,
            String customerEmail,
            String status,
            String fulfillmentStatus,
            String currency,
            long totalCents,
            int itemCount,
            OffsetDateTime createdAt,
            OffsetDateTime paidAt
    ) {
    }

    public record OrderLine(
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

    public record OrderDetail(
            OrderSummary summary,
            String phone,
            String addressLine1,
            String addressLine2,
            String suburb,
            String state,
            String postcode,
            String countryCode,
            String carrier,
            String trackingNumber,
            String paymentReference,
            String paymentStatus,
            String invoiceNumber,
            List<OrderLine> items
    ) {
    }

    public record Product(
            UUID productId,
            UUID variantId,
            String category,
            String brand,
            String name,
            String subtype,
            String sku,
            String status,
            boolean active,
            long priceCents,
            String currency
    ) {
    }

    public record Inventory(
            UUID itemId,
            String sku,
            String brand,
            String productName,
            String productType,
            boolean linkedToCatalog,
            String locationCode,
            String locationName,
            int onHand,
            int reserved,
            int available,
            int reorderLevel,
            OffsetDateTime updatedAt
    ) {
    }

    public record Movement(
            UUID id,
            String sku,
            String movementType,
            int onHandDelta,
            int reservedDelta,
            String reason,
            String performedBy,
            OffsetDateTime createdAt
    ) {
    }

    public record FulfillmentUpdate(String status, String carrier, String trackingNumber) {
    }

    public record ProductUpdate(Long priceCents, String status, Boolean active) {
    }

    public record InventoryAdjustment(String movementType, int quantityDelta, String reason, Integer reorderLevel) {
    }

    public record InventoryItemCreate(
            String sku,
            String brand,
            String productName,
            String categoryCode,
            String productType,
            int initialQuantity,
            int reorderLevel,
            String reason
    ) {
    }
}
