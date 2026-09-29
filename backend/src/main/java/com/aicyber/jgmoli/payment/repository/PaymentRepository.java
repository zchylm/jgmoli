package com.aicyber.jgmoli.payment.repository;

import com.aicyber.jgmoli.payment.dto.PaymentResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextPaymentSequence() {
        return jdbcTemplate.queryForObject("SELECT nextval('payment_reference_seq')", Long.class);
    }

    public Optional<OrderForPayment> lockOrder(UUID userId, String orderReference) {
        return jdbcTemplate.query("""
                SELECT id, order_reference, status, total_cents, currency
                FROM sales_orders
                WHERE user_id = ? AND order_reference = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> new OrderForPayment(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("status"),
                resultSet.getLong("total_cents"),
                resultSet.getString("currency")
        ), userId, orderReference).stream().findFirst();
    }

    public Optional<PaymentResponse> findByIdempotencyKey(UUID orderId, String key) {
        return find("WHERE p.order_id = ? AND p.idempotency_key = ?", orderId, key);
    }

    public Optional<PaymentResponse> findSucceeded(UUID orderId) {
        return find("WHERE p.order_id = ? AND p.status = 'SUCCEEDED'", orderId);
    }

    private Optional<PaymentResponse> find(String whereClause, Object... arguments) {
        return jdbcTemplate.query("""
                SELECT p.id, p.payment_reference, o.order_reference, o.status AS order_status,
                       p.status AS payment_status, p.amount_cents, p.currency, p.completed_at
                FROM payments p
                JOIN sales_orders o ON o.id = p.order_id
                %s
                ORDER BY p.created_at DESC
                LIMIT 1
                """.formatted(whereClause), (resultSet, rowNumber) -> new PaymentResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("payment_reference"),
                resultSet.getString("order_reference"),
                resultSet.getString("order_status"),
                resultSet.getString("payment_status"),
                resultSet.getLong("amount_cents"),
                resultSet.getString("currency"),
                resultSet.getObject("completed_at", OffsetDateTime.class)
        ), arguments).stream().findFirst();
    }

    public PaymentResponse completeDemoPayment(OrderForPayment order, String paymentReference, String key) {
        UUID paymentId = UUID.randomUUID();
        OffsetDateTime completedAt = OffsetDateTime.now();
        jdbcTemplate.update("""
                INSERT INTO payments
                    (id, order_id, payment_reference, provider, provider_payment_id, amount_cents,
                     currency, status, idempotency_key, updated_at, completed_at)
                VALUES (?, ?, ?, 'DEMO', ?, ?, ?, 'SUCCEEDED', ?, ?, ?)
                """, paymentId, order.id(), paymentReference, "demo_" + paymentId,
                order.totalCents(), order.currency(), key, completedAt, completedAt);
        jdbcTemplate.update("""
                UPDATE sales_orders
                SET status = 'PAID', paid_at = ?, updated_at = ?
                WHERE id = ?
                """, completedAt, completedAt, order.id());
        return new PaymentResponse(paymentId, paymentReference, order.orderReference(), "PAID", "SUCCEEDED",
                order.totalCents(), order.currency(), completedAt);
    }

    public record OrderForPayment(UUID id, String orderReference, String status, long totalCents, String currency) {
    }
}
