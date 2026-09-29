package com.aicyber.jgmoli.admin.repository;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminDtos.Overview overview() {
        Map<String, Object> totals = jdbcTemplate.queryForMap("""
                SELECT
                    COUNT(*) FILTER (WHERE created_at::date = CURRENT_DATE) AS orders_today,
                    COALESCE(SUM(total_cents) FILTER (WHERE status = 'PAID' AND paid_at::date = CURRENT_DATE), 0) AS revenue_today,
                    COUNT(*) FILTER (WHERE status = 'AWAITING_PAYMENT') AS awaiting_payment,
                    COUNT(*) FILTER (WHERE status = 'PAID' AND fulfillment_status IN ('UNFULFILLED', 'PROCESSING')) AS needs_fulfilment
                FROM sales_orders
                """);
        Long lowStock = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM inventory_balances
                WHERE reorder_level > 0 AND (on_hand - reserved) <= reorder_level
                """, Long.class);
        return new AdminDtos.Overview(
                ((Number) totals.get("orders_today")).longValue(),
                ((Number) totals.get("revenue_today")).longValue(),
                ((Number) totals.get("awaiting_payment")).longValue(),
                ((Number) totals.get("needs_fulfilment")).longValue(),
                lowStock == null ? 0 : lowStock,
                orders("", "", 0, 6)
        );
    }

    public List<AdminDtos.OrderSummary> orders(String query, String status, int offset, int limit) {
        String search = "%" + query.toLowerCase() + "%";
        return jdbcTemplate.query("""
                SELECT o.id, o.order_reference, d.recipient_name, o.customer_email_snapshot,
                       o.status, o.fulfillment_status, o.currency, o.total_cents,
                       COALESCE(SUM(oi.quantity), 0)::int AS item_count, o.created_at, o.paid_at
                FROM sales_orders o
                JOIN sales_order_delivery d ON d.order_id = o.id
                LEFT JOIN sales_order_items oi ON oi.order_id = o.id
                WHERE (? = '' OR LOWER(o.order_reference) LIKE ? OR LOWER(o.customer_email_snapshot) LIKE ?
                       OR LOWER(d.recipient_name) LIKE ? OR EXISTS (
                           SELECT 1 FROM sales_order_items search_item
                           WHERE search_item.order_id = o.id AND LOWER(search_item.sku_snapshot) LIKE ?))
                  AND (? = '' OR o.status = ? OR o.fulfillment_status = ?)
                GROUP BY o.id, d.recipient_name
                ORDER BY o.created_at DESC
                OFFSET ? LIMIT ?
                """, (rs, row) -> new AdminDtos.OrderSummary(
                rs.getObject("id", UUID.class), rs.getString("order_reference"),
                rs.getString("recipient_name"), rs.getString("customer_email_snapshot"),
                rs.getString("status"), rs.getString("fulfillment_status"), rs.getString("currency"),
                rs.getLong("total_cents"), rs.getInt("item_count"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("paid_at", OffsetDateTime.class)
        ), query, search, search, search, search, status, status, status, offset, limit);
    }

    public Optional<AdminDtos.OrderDetail> order(String reference) {
        return jdbcTemplate.query("""
                SELECT o.id, o.order_reference, d.recipient_name, o.customer_email_snapshot,
                       o.status, o.fulfillment_status, o.currency, o.total_cents,
                       (SELECT COALESCE(SUM(quantity), 0) FROM sales_order_items WHERE order_id = o.id)::int AS item_count,
                       o.created_at, o.paid_at, d.phone, d.address_line_1, d.address_line_2,
                       d.suburb, d.state, d.postcode, d.country_code, o.carrier, o.tracking_number,
                       p.payment_reference, p.status AS payment_status, i.invoice_number
                FROM sales_orders o
                JOIN sales_order_delivery d ON d.order_id = o.id
                LEFT JOIN payments p ON p.order_id = o.id AND p.status = 'SUCCEEDED'
                LEFT JOIN sales_invoices i ON i.order_id = o.id
                WHERE o.order_reference = ?
                """, (rs, row) -> {
            UUID orderId = rs.getObject("id", UUID.class);
            AdminDtos.OrderSummary summary = new AdminDtos.OrderSummary(
                    orderId, rs.getString("order_reference"), rs.getString("recipient_name"),
                    rs.getString("customer_email_snapshot"), rs.getString("status"),
                    rs.getString("fulfillment_status"), rs.getString("currency"), rs.getLong("total_cents"),
                    rs.getInt("item_count"), rs.getObject("created_at", OffsetDateTime.class),
                    rs.getObject("paid_at", OffsetDateTime.class));
            return new AdminDtos.OrderDetail(summary, rs.getString("phone"), rs.getString("address_line_1"),
                    rs.getString("address_line_2"), rs.getString("suburb"), rs.getString("state"),
                    rs.getString("postcode"), rs.getString("country_code"), rs.getString("carrier"),
                    rs.getString("tracking_number"), rs.getString("payment_reference"),
                    rs.getString("payment_status"), rs.getString("invoice_number"), orderLines(orderId));
        }, reference).stream().findFirst();
    }

    private List<AdminDtos.OrderLine> orderLines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT sku_snapshot, brand_snapshot, product_name_snapshot, variant_name_snapshot,
                       quantity, unit_price_inc_gst_cents, gst_cents, line_total_cents
                FROM sales_order_items WHERE order_id = ? ORDER BY line_number
                """, (rs, row) -> new AdminDtos.OrderLine(rs.getString("sku_snapshot"),
                rs.getString("brand_snapshot"), rs.getString("product_name_snapshot"),
                rs.getString("variant_name_snapshot"), rs.getInt("quantity"),
                rs.getLong("unit_price_inc_gst_cents"), rs.getLong("gst_cents"),
                rs.getLong("line_total_cents")), orderId);
    }

    public List<AdminDtos.Product> products() {
        return jdbcTemplate.query("""
                SELECT p.id product_id, v.id variant_id, c.name category, p.brand, p.name, p.subtype,
                       v.sku, p.status, v.active, v.price_cents, v.currency
                FROM products p
                JOIN product_categories c ON c.id = p.category_id
                JOIN product_variants v ON v.product_id = p.id
                ORDER BY c.sort_order, p.sort_order, p.name
                """, (rs, row) -> new AdminDtos.Product(rs.getObject("product_id", UUID.class),
                rs.getObject("variant_id", UUID.class), rs.getString("category"), rs.getString("brand"),
                rs.getString("name"), rs.getString("subtype"), rs.getString("sku"), rs.getString("status"),
                rs.getBoolean("active"), rs.getLong("price_cents"), rs.getString("currency")));
    }

    public Optional<AdminDtos.Product> product(UUID variantId) {
        return products().stream().filter(item -> item.variantId().equals(variantId)).findFirst();
    }

    public List<AdminDtos.Inventory> inventory() {
        return jdbcTemplate.query("""
                SELECT item.id item_id, item.sku, item.brand, item.name product_name,
                       item.item_type product_type, item.catalog_variant_id IS NOT NULL linked_to_catalog,
                       l.code location_code,
                       l.name location_name, b.on_hand, b.reserved, b.on_hand - b.reserved available,
                       b.reorder_level, b.updated_at
                FROM inventory_balances b
                JOIN inventory_items item ON item.id = b.item_id
                JOIN inventory_locations l ON l.id = b.location_id
                WHERE item.active = TRUE
                ORDER BY item.name
                """, (rs, row) -> new AdminDtos.Inventory(rs.getObject("item_id", UUID.class),
                rs.getString("sku"), rs.getString("brand"), rs.getString("product_name"),
                rs.getString("product_type"), rs.getBoolean("linked_to_catalog"),
                rs.getString("location_code"), rs.getString("location_name"), rs.getInt("on_hand"),
                rs.getInt("reserved"), rs.getInt("available"), rs.getInt("reorder_level"),
                rs.getObject("updated_at", OffsetDateTime.class)));
    }

    public Optional<AdminDtos.Inventory> inventory(UUID itemId) {
        return inventory().stream().filter(item -> item.itemId().equals(itemId)).findFirst();
    }

    public List<AdminDtos.Movement> movements() {
        return jdbcTemplate.query("""
                SELECT m.id, item.sku, m.movement_type, m.on_hand_delta, m.reserved_delta,
                       m.reason, u.display_name performed_by, m.created_at
                FROM inventory_movements m
                JOIN inventory_items item ON item.id = m.item_id
                JOIN users u ON u.id = m.performed_by
                ORDER BY m.created_at DESC LIMIT 50
                """, (rs, row) -> new AdminDtos.Movement(rs.getObject("id", UUID.class), rs.getString("sku"),
                rs.getString("movement_type"), rs.getInt("on_hand_delta"), rs.getInt("reserved_delta"),
                rs.getString("reason"), rs.getString("performed_by"),
                rs.getObject("created_at", OffsetDateTime.class)));
    }

    public JdbcTemplate jdbc() {
        return jdbcTemplate;
    }
}
