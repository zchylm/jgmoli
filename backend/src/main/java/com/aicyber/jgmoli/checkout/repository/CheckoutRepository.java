package com.aicyber.jgmoli.checkout.repository;

import com.aicyber.jgmoli.checkout.dto.DeliveryRequest;
import com.aicyber.jgmoli.checkout.dto.OrderLineResponse;
import com.aicyber.jgmoli.checkout.dto.OrderResponse;
import com.aicyber.jgmoli.checkout.model.CheckoutVariant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CheckoutRepository {

    private final JdbcTemplate jdbcTemplate;

    public CheckoutRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextOrderSequence() {
        return jdbcTemplate.queryForObject("SELECT nextval('sales_order_reference_seq')", Long.class);
    }

    public List<CheckoutVariant> findActiveVariants(List<UUID> variantIds) {
        String placeholders = String.join(",", variantIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("""
                SELECT p.id AS product_id, v.id AS variant_id, v.sku, p.brand,
                       p.name AS product_name, v.name AS variant_name, v.price_cents,
                       v.currency, v.gst_rate, v.price_includes_gst
                FROM product_variants v
                JOIN products p ON p.id = v.product_id
                WHERE v.id IN (%s) AND v.active = TRUE AND p.status = 'ACTIVE'
                """.formatted(placeholders), (resultSet, rowNumber) -> new CheckoutVariant(
                resultSet.getObject("product_id", UUID.class),
                resultSet.getObject("variant_id", UUID.class),
                resultSet.getString("sku"),
                resultSet.getString("brand"),
                resultSet.getString("product_name"),
                resultSet.getString("variant_name"),
                resultSet.getLong("price_cents"),
                resultSet.getString("currency"),
                resultSet.getBigDecimal("gst_rate"),
                resultSet.getBoolean("price_includes_gst")
        ), variantIds.toArray());
    }

    public Optional<StoredOrder> findByIdempotencyKey(UUID userId, String key) {
        return jdbcTemplate.query("""
                SELECT id, order_reference, request_fingerprint
                FROM sales_orders
                WHERE user_id = ? AND checkout_idempotency_key = ?
                """, (resultSet, rowNumber) -> new StoredOrder(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("request_fingerprint")
        ), userId, key).stream().findFirst();
    }

    public boolean createOrder(
            UUID orderId,
            String orderReference,
            UUID userId,
            String source,
            String currency,
            long subtotalExGstCents,
            long gstCents,
            long deliveryCents,
            long totalCents,
            String customerEmail,
            String idempotencyKey,
            String requestFingerprint
    ) {
        return jdbcTemplate.update("""
                INSERT INTO sales_orders
                    (id, order_reference, user_id, source, currency, subtotal_ex_gst_cents,
                     gst_cents, delivery_cents, total_cents, customer_email_snapshot,
                     checkout_idempotency_key, request_fingerprint)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, checkout_idempotency_key) DO NOTHING
                """, orderId, orderReference, userId, source, currency, subtotalExGstCents,
                gstCents, deliveryCents, totalCents, customerEmail, idempotencyKey, requestFingerprint) == 1;
    }

    public void createOrderLine(
            UUID orderId,
            int lineNumber,
            CheckoutVariant variant,
            int quantity,
            long unitPriceIncGstCents,
            long unitPriceExGstCents,
            long gstCents,
            long lineTotalCents
    ) {
        jdbcTemplate.update("""
                INSERT INTO sales_order_items
                    (id, order_id, line_number, product_id, variant_id, sku_snapshot, brand_snapshot,
                     product_name_snapshot, variant_name_snapshot, quantity, unit_price_inc_gst_cents,
                     unit_price_ex_gst_cents, gst_cents, line_total_cents)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), orderId, lineNumber, variant.productId(), variant.variantId(),
                variant.sku(), variant.brand(), variant.productName(), variant.variantName(), quantity,
                unitPriceIncGstCents, unitPriceExGstCents, gstCents, lineTotalCents);
    }

    public void createDelivery(UUID orderId, DeliveryRequest delivery) {
        jdbcTemplate.update("""
                INSERT INTO sales_order_delivery
                    (order_id, recipient_name, phone, address_line_1, address_line_2,
                     suburb, state, postcode, country_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, orderId, delivery.recipientName(), delivery.phone(), delivery.addressLine1(),
                delivery.addressLine2(), delivery.suburb(), delivery.state(), delivery.postcode(),
                delivery.countryCode());
    }

    public Optional<OrderResponse> findOrder(UUID userId, String orderReference) {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_reference, o.source, o.status, o.currency,
                       o.subtotal_ex_gst_cents, o.gst_cents, o.delivery_cents, o.total_cents,
                       o.customer_email_snapshot, o.created_at, o.paid_at,
                       d.recipient_name, d.phone, d.address_line_1, d.address_line_2,
                       d.suburb, d.state, d.postcode, d.country_code
                FROM sales_orders o
                JOIN sales_order_delivery d ON d.order_id = o.id
                WHERE o.user_id = ? AND o.order_reference = ?
                """, (resultSet, rowNumber) -> mapOrder(resultSet), userId, orderReference).stream().findFirst().map(order -> new OrderResponse(
                order.id(), order.orderReference(), order.source(), order.status(), order.currency(),
                order.subtotalExGstCents(), order.gstCents(), order.deliveryCents(), order.totalCents(),
                order.customerEmail(), order.delivery(), findOrderLines(order.id()), order.createdAt(), order.paidAt()
        ));
    }

    public List<OrderResponse> findOrders(UUID userId) {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_reference, o.source, o.status, o.currency,
                       o.subtotal_ex_gst_cents, o.gst_cents, o.delivery_cents, o.total_cents,
                       o.customer_email_snapshot, o.created_at, o.paid_at,
                       d.recipient_name, d.phone, d.address_line_1, d.address_line_2,
                       d.suburb, d.state, d.postcode, d.country_code
                FROM sales_orders o
                JOIN sales_order_delivery d ON d.order_id = o.id
                WHERE o.user_id = ?
                ORDER BY o.created_at DESC
                """, (resultSet, rowNumber) -> mapOrder(resultSet), userId).stream()
                .map(order -> new OrderResponse(
                        order.id(), order.orderReference(), order.source(), order.status(), order.currency(),
                        order.subtotalExGstCents(), order.gstCents(), order.deliveryCents(), order.totalCents(),
                        order.customerEmail(), order.delivery(), findOrderLines(order.id()), order.createdAt(), order.paidAt()
                ))
                .toList();
    }

    private OrderResponse mapOrder(ResultSet resultSet) throws SQLException {
        return new OrderResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("source"),
                resultSet.getString("status"),
                resultSet.getString("currency"),
                resultSet.getLong("subtotal_ex_gst_cents"),
                resultSet.getLong("gst_cents"),
                resultSet.getLong("delivery_cents"),
                resultSet.getLong("total_cents"),
                resultSet.getString("customer_email_snapshot"),
                new DeliveryRequest(
                        resultSet.getString("recipient_name"), resultSet.getString("phone"),
                        resultSet.getString("address_line_1"), resultSet.getString("address_line_2"),
                        resultSet.getString("suburb"), resultSet.getString("state"),
                        resultSet.getString("postcode"), resultSet.getString("country_code")
                ),
                List.of(),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("paid_at", OffsetDateTime.class)
        );
    }

    private List<OrderLineResponse> findOrderLines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT variant_id, sku_snapshot, brand_snapshot, product_name_snapshot,
                       variant_name_snapshot, quantity, unit_price_inc_gst_cents, gst_cents, line_total_cents
                FROM sales_order_items
                WHERE order_id = ?
                ORDER BY line_number
                """, (resultSet, rowNumber) -> new OrderLineResponse(
                resultSet.getObject("variant_id", UUID.class),
                resultSet.getString("sku_snapshot"),
                resultSet.getString("brand_snapshot"),
                resultSet.getString("product_name_snapshot"),
                resultSet.getString("variant_name_snapshot"),
                resultSet.getInt("quantity"),
                resultSet.getLong("unit_price_inc_gst_cents"),
                resultSet.getLong("gst_cents"),
                resultSet.getLong("line_total_cents")
        ), orderId);
    }

    public record StoredOrder(UUID id, String orderReference, String requestFingerprint) {
    }
}
