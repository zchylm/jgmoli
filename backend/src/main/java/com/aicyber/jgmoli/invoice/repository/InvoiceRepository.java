package com.aicyber.jgmoli.invoice.repository;

import com.aicyber.jgmoli.invoice.config.InvoiceBusinessDetails;
import com.aicyber.jgmoli.invoice.dto.InvoiceAddressResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceLineResponse;
import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import com.aicyber.jgmoli.invoice.model.InvoiceSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvoiceRepository {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long nextInvoiceSequence() {
        return jdbcTemplate.queryForObject("SELECT nextval('tax_invoice_reference_seq')", Long.class);
    }

    public Optional<InvoiceSource> sourceForIssue(UUID userId, String orderReference) {
        return jdbcTemplate.query("""
                SELECT o.id AS order_id, p.id AS payment_id, o.order_reference, p.payment_reference,
                       o.status AS order_status, p.status AS payment_status,
                       d.recipient_name, o.customer_email_snapshot,
                       d.address_line_1, d.address_line_2, d.suburb, d.state, d.postcode, d.country_code,
                       o.currency, o.subtotal_ex_gst_cents, o.gst_cents, o.delivery_cents,
                       o.total_cents, p.amount_cents
                FROM sales_orders o
                JOIN payments p ON p.order_id = o.id AND p.status = 'SUCCEEDED'
                JOIN sales_order_delivery d ON d.order_id = o.id
                WHERE o.user_id = ? AND o.order_reference = ?
                """, (resultSet, rowNumber) -> new InvoiceSource(
                resultSet.getObject("order_id", UUID.class),
                resultSet.getObject("payment_id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("payment_reference"),
                resultSet.getString("order_status"),
                resultSet.getString("payment_status"),
                resultSet.getString("recipient_name"),
                resultSet.getString("customer_email_snapshot"),
                new InvoiceAddressResponse(
                        resultSet.getString("address_line_1"), resultSet.getString("address_line_2"),
                        resultSet.getString("suburb"), resultSet.getString("state"),
                        resultSet.getString("postcode"), resultSet.getString("country_code")
                ),
                resultSet.getString("currency"),
                resultSet.getLong("subtotal_ex_gst_cents"), resultSet.getLong("gst_cents"),
                resultSet.getLong("delivery_cents"), resultSet.getLong("total_cents"),
                resultSet.getLong("amount_cents")
        ), userId, orderReference).stream().findFirst();
    }

    public List<InvoiceLineResponse> sourceLines(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT line_number, sku_snapshot,
                       CONCAT(brand_snapshot, ' ', product_name_snapshot,
                              CASE WHEN variant_name_snapshot = 'Standard' THEN '' ELSE CONCAT(' · ', variant_name_snapshot) END) AS description,
                       quantity, unit_price_ex_gst_cents, gst_cents, line_total_cents
                FROM sales_order_items
                WHERE order_id = ?
                ORDER BY line_number
                """, (resultSet, rowNumber) -> new InvoiceLineResponse(
                resultSet.getInt("line_number"), resultSet.getString("sku_snapshot"),
                resultSet.getString("description"), resultSet.getInt("quantity"),
                resultSet.getLong("unit_price_ex_gst_cents"), resultSet.getLong("gst_cents"),
                resultSet.getLong("line_total_cents"), true
        ), orderId);
    }

    public void create(
            UUID invoiceId,
            String invoiceNumber,
            InvoiceSource source,
            InvoiceBusinessDetails seller,
            OffsetDateTime issuedAt,
            List<InvoiceLineResponse> lines
    ) {
        InvoiceAddressResponse address = source.buyerAddress();
        jdbcTemplate.update("""
                INSERT INTO sales_invoices
                    (id, invoice_number, order_id, payment_id, seller_legal_name, seller_trading_name,
                     seller_abn, seller_address, seller_email, seller_phone, buyer_name, buyer_email,
                     buyer_address_line_1, buyer_address_line_2, buyer_suburb, buyer_state,
                     buyer_postcode, buyer_country_code, currency, subtotal_ex_gst_cents, gst_cents,
                     delivery_cents, total_cents, amount_paid_cents, issued_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, invoiceId, invoiceNumber, source.orderId(), source.paymentId(), seller.legalName(),
                seller.tradingName(), seller.abn(), seller.address(), seller.email(), seller.phone(),
                source.buyerName(), source.buyerEmail(), address.addressLine1(), address.addressLine2(),
                address.suburb(), address.state(), address.postcode(), address.countryCode(), source.currency(),
                source.subtotalExGstCents(), source.gstCents(), source.deliveryCents(), source.totalCents(),
                source.amountPaidCents(), issuedAt);
        for (InvoiceLineResponse line : lines) {
            jdbcTemplate.update("""
                    INSERT INTO sales_invoice_lines
                        (id, invoice_id, line_number, sku_snapshot, description_snapshot, quantity,
                         unit_price_ex_gst_cents, gst_cents, line_total_inc_gst_cents, taxable)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), invoiceId, line.lineNumber(), line.sku(), line.description(),
                    line.quantity(), line.unitPriceExGstCents(), line.gstCents(),
                    line.lineTotalIncGstCents(), line.taxable());
        }
    }

    public Optional<InvoiceResponse> findByOrder(UUID userId, String orderReference) {
        return find("WHERE o.user_id = ? AND o.order_reference = ?", userId, orderReference);
    }

    private Optional<InvoiceResponse> find(String whereClause, Object... arguments) {
        return jdbcTemplate.query("""
                SELECT i.id, i.invoice_number, i.document_type, i.status,
                       o.order_reference, p.payment_reference,
                       i.seller_legal_name, i.seller_trading_name, i.seller_abn,
                       i.seller_address, i.seller_email, i.seller_phone,
                       i.buyer_name, i.buyer_email, i.buyer_address_line_1,
                       i.buyer_address_line_2, i.buyer_suburb, i.buyer_state,
                       i.buyer_postcode, i.buyer_country_code, i.currency,
                       i.subtotal_ex_gst_cents, i.gst_cents, i.delivery_cents,
                       i.total_cents, i.amount_paid_cents, i.issued_at
                FROM sales_invoices i
                JOIN sales_orders o ON o.id = i.order_id
                JOIN payments p ON p.id = i.payment_id
                %s
                """.formatted(whereClause), (resultSet, rowNumber) -> new InvoiceResponse(
                resultSet.getObject("id", UUID.class), resultSet.getString("invoice_number"),
                resultSet.getString("document_type"), resultSet.getString("status"),
                resultSet.getString("order_reference"), resultSet.getString("payment_reference"),
                resultSet.getString("seller_legal_name"), resultSet.getString("seller_trading_name"),
                resultSet.getString("seller_abn"), resultSet.getString("seller_address"),
                resultSet.getString("seller_email"), resultSet.getString("seller_phone"),
                resultSet.getString("buyer_name"), resultSet.getString("buyer_email"),
                new InvoiceAddressResponse(
                        resultSet.getString("buyer_address_line_1"), resultSet.getString("buyer_address_line_2"),
                        resultSet.getString("buyer_suburb"), resultSet.getString("buyer_state"),
                        resultSet.getString("buyer_postcode"), resultSet.getString("buyer_country_code")
                ),
                resultSet.getString("currency"), resultSet.getLong("subtotal_ex_gst_cents"),
                resultSet.getLong("gst_cents"), resultSet.getLong("delivery_cents"),
                resultSet.getLong("total_cents"), resultSet.getLong("amount_paid_cents"),
                resultSet.getObject("issued_at", OffsetDateTime.class), List.of()
        ), arguments).stream().findFirst().map(invoice -> new InvoiceResponse(
                invoice.id(), invoice.invoiceNumber(), invoice.documentType(), invoice.status(),
                invoice.orderReference(), invoice.paymentReference(), invoice.sellerLegalName(),
                invoice.sellerTradingName(), invoice.sellerAbn(), invoice.sellerAddress(),
                invoice.sellerEmail(), invoice.sellerPhone(), invoice.buyerName(), invoice.buyerEmail(),
                invoice.buyerAddress(), invoice.currency(), invoice.subtotalExGstCents(), invoice.gstCents(),
                invoice.deliveryCents(), invoice.totalCents(), invoice.amountPaidCents(), invoice.issuedAt(),
                findLines(invoice.id())
        ));
    }

    private List<InvoiceLineResponse> findLines(UUID invoiceId) {
        return jdbcTemplate.query("""
                SELECT line_number, sku_snapshot, description_snapshot, quantity,
                       unit_price_ex_gst_cents, gst_cents, line_total_inc_gst_cents, taxable
                FROM sales_invoice_lines
                WHERE invoice_id = ?
                ORDER BY line_number
                """, (resultSet, rowNumber) -> new InvoiceLineResponse(
                resultSet.getInt("line_number"), resultSet.getString("sku_snapshot"),
                resultSet.getString("description_snapshot"), resultSet.getInt("quantity"),
                resultSet.getLong("unit_price_ex_gst_cents"), resultSet.getLong("gst_cents"),
                resultSet.getLong("line_total_inc_gst_cents"), resultSet.getBoolean("taxable")
        ), invoiceId);
    }
}
