package com.aicyber.jgmoli.payment.service;

import com.aicyber.jgmoli.invoice.service.InvoiceService;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.UUID;

@Service
public class StripePaymentService {
    private final JdbcTemplate db;
    private final InvoiceService invoices;
    private final TransactionTemplate transactions;
    private final String key;
    private final String webhookSecret;
    private final String returnUrl;
    private final StripePaymentSettings settings;

    public StripePaymentService(JdbcTemplate db, InvoiceService invoices, TransactionTemplate transactions, StripePaymentSettings settings) {
        this.db = db; this.invoices = invoices; this.transactions = transactions; this.settings = settings;
        this.key = settings.secretKey(); this.webhookSecret = settings.webhookSecret(); this.returnUrl = settings.returnUrl();
    }

    public Checkout create(UUID userId, String reference) {
        settings.requireConfigured();
        if (webhookSecret.isBlank()) throw new IllegalStateException("Stripe webhook is not configured.");
        if (reference == null || !reference.matches("JGM-[0-9]{8}-[0-9]{6,}")) throw new IllegalArgumentException("Invalid order.");
        Prepared prepared = transactions.execute(status -> prepare(userId, reference));
        if (prepared == null) throw new IllegalStateException("Unable to prepare payment.");
        Order order = prepared.order();
        UUID paymentId = prepared.attempt().id();
        String sessionId = prepared.attempt().sessionId();
        try {
            StripeClient client = new StripeClient(key);
            Session session;
            if (sessionId == null) {
                var params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
                        .setWalletOptions(SessionCreateParams.WalletOptions.builder()
                                .setLink(SessionCreateParams.WalletOptions.Link.builder()
                                        .setDisplay(SessionCreateParams.WalletOptions.Link.Display.NEVER).build()).build())
                        .setSuccessUrl(returnUrl + "?stripe_return=success&order=" + reference)
                        .setCancelUrl(returnUrl + "?stripe_return=cancelled&order=" + reference)
                        .setClientReferenceId(reference).setCustomerEmail(order.email())
                        .putMetadata("jgmoli_payment_id", paymentId.toString())
                        .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(order.currency().toLowerCase(Locale.ROOT)).setUnitAmount(order.total())
                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName("JG MOLI order " + reference).build()).build()).build()).build();
                session = client.v1().checkout().sessions().create(params, RequestOptions.builder().setIdempotencyKey("jgmoli-checkout-" + paymentId).build());
                int updated = db.update("UPDATE payments SET stripe_session_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND (stripe_session_id IS NULL OR stripe_session_id = ?)", session.getId(), paymentId, session.getId());
                if (updated != 1) throw new IllegalStateException("Stripe session mismatch.");
            } else session = client.v1().checkout().sessions().retrieve(sessionId);
            if ("expired".equals(session.getStatus())) {
                db.update("UPDATE payments SET status='CANCELLED', updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='CREATED'", paymentId);
                throw new IllegalStateException("This payment session has expired. Please try again to open a new payment page.");
            }
            if (session.getUrl() == null) throw new IllegalStateException("This payment session is no longer available.");
            return new Checkout(session.getUrl());
        } catch (StripeException e) { throw new IllegalStateException("Stripe checkout is temporarily unavailable.", e); }
    }

    private Prepared prepare(UUID userId, String reference) {
        var orders = db.query("SELECT id, status, total_cents, currency, customer_email_snapshot FROM sales_orders WHERE user_id = ? AND order_reference = ? FOR UPDATE",
                (rs, n) -> new Order(rs.getObject(1, UUID.class), rs.getString(2), rs.getLong(3), rs.getString(4).trim(), rs.getString(5)), userId, reference);
        if (orders.isEmpty()) throw new IllegalArgumentException("Order not found.");
        Order order = orders.get(0);
        if (!"AWAITING_PAYMENT".equals(order.status()) && !"PAYMENT_FAILED".equals(order.status())) throw new IllegalStateException("Order is not awaiting payment.");
        if (Boolean.TRUE.equals(db.queryForObject("SELECT EXISTS (SELECT 1 FROM payments WHERE order_id=? AND provider='STRIPE' AND stripe_livemode <> ?)", Boolean.class, order.id(), settings.live()))) {
            throw new IllegalStateException("This order belongs to a different payment environment. Please create a new order.");
        }
        Boolean unresolved = db.queryForObject("SELECT EXISTS (SELECT 1 FROM payments WHERE order_id=? AND provider='STRIPE' AND status IN ('CREATED','PROCESSING') AND stripe_session_id IS NULL AND created_at < CURRENT_TIMESTAMP - INTERVAL '23 hours')", Boolean.class, order.id());
        if (Boolean.TRUE.equals(unresolved)) throw new IllegalStateException("This payment attempt needs review before another payment can be started. Please contact support.");
        var existing = db.query("SELECT id, stripe_session_id FROM payments WHERE order_id = ? AND provider = 'STRIPE' AND status IN ('CREATED','PROCESSING') FOR UPDATE",
                (rs, n) -> new Attempt(rs.getObject(1, UUID.class), rs.getString(2)), order.id());
        UUID paymentId = existing.isEmpty() ? UUID.randomUUID() : existing.get(0).id();
        String sessionId = existing.isEmpty() ? null : existing.get(0).sessionId();
        if (existing.isEmpty()) {
            long sequence = db.queryForObject("SELECT nextval('payment_reference_seq')", Long.class);
            db.update("INSERT INTO payments(id, order_id, payment_reference, provider, amount_cents, currency, status, idempotency_key, stripe_livemode) VALUES (?, ?, ?, 'STRIPE', ?, ?, 'CREATED', ?, ?)",
                    paymentId, order.id(), "JGM-STRIPE-" + sequence, order.total(), order.currency(), paymentId.toString(), settings.live());
        }
        return new Prepared(order, new Attempt(paymentId, sessionId));
    }

    @Transactional
    public void webhook(String payload, String signature) {
        settings.requireConfigured();
        if (webhookSecret.isBlank()) throw new IllegalStateException("Stripe webhook is not configured.");
        Event event;
        try { event = Webhook.constructEvent(payload, signature, webhookSecret); }
        catch (SignatureVerificationException e) { throw new IllegalArgumentException("Invalid Stripe signature."); }
        if (event.getLivemode() == null || event.getLivemode() != settings.live()) throw new IllegalArgumentException("Stripe event environment does not match.");
        if (!event.getType().equals("checkout.session.completed") && !event.getType().equals("checkout.session.async_payment_succeeded") && !event.getType().equals("checkout.session.async_payment_failed") && !event.getType().equals("checkout.session.expired")) return;
        var object = event.getDataObjectDeserializer().getObject().orElseThrow(() -> new IllegalArgumentException("Unsupported Stripe event format."));
        if (!(object instanceof Session session)) throw new IllegalArgumentException("Invalid Stripe session.");
        String metadataId = session.getMetadata() == null ? null : session.getMetadata().get("jgmoli_payment_id");
        if (metadataId == null) return;
        if (session.getLivemode() == null || session.getLivemode() != settings.live()) throw new IllegalArgumentException("Stripe session environment does not match.");
        if (metadataId != null) {
            UUID paymentId;
            try { paymentId = UUID.fromString(metadataId); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid payment reference."); }
            int linked = db.update("UPDATE payments p SET stripe_session_id = ? FROM sales_orders o WHERE p.id = ? AND p.order_id = o.id AND p.provider = 'STRIPE' AND p.stripe_livemode = ? AND o.order_reference = ? AND (p.stripe_session_id IS NULL OR p.stripe_session_id = ?)", session.getId(), paymentId, settings.live(), session.getClientReferenceId(), session.getId());
            if (linked == 0) return;
        }
        var rows = db.query("SELECT p.id, p.amount_cents, p.currency, p.status, o.user_id, o.order_reference, o.status FROM payments p JOIN sales_orders o ON o.id = p.order_id WHERE p.stripe_session_id = ? FOR UPDATE OF p, o",
                (rs, n) -> new Settlement(rs.getObject(1, UUID.class), rs.getLong(2), rs.getString(3).trim(), rs.getString(4), rs.getObject(5, UUID.class), rs.getString(6), rs.getString(7)), session.getId());
        if (rows.isEmpty()) throw new IllegalArgumentException("Unknown Stripe session.");
        Settlement p = rows.get(0);
        if (session.getAmountTotal() == null || session.getAmountTotal() != p.amount() || session.getCurrency() == null || !p.currency().equalsIgnoreCase(session.getCurrency())) throw new IllegalArgumentException("Stripe amount or currency mismatch.");
        if ("SUCCEEDED".equals(p.status())) return;
        if ((event.getType().equals("checkout.session.completed") || event.getType().equals("checkout.session.async_payment_succeeded")) && "paid".equals(session.getPaymentStatus())) {
            if (!"AWAITING_PAYMENT".equals(p.orderStatus()) && !"PAYMENT_FAILED".equals(p.orderStatus())) throw new IllegalStateException("Order cannot be settled.");
            db.update("UPDATE payments SET status='SUCCEEDED', provider_payment_id=?, completed_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?", session.getPaymentIntent(), p.id());
            db.update("UPDATE sales_orders SET status='PAID', paid_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE order_reference=?", p.reference());
            invoices.issueForSuccessfulPayment(p.userId(), p.reference());
        } else if (event.getType().equals("checkout.session.expired") || event.getType().equals("checkout.session.async_payment_failed")) {
            db.update("UPDATE payments SET status=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('CREATED','PROCESSING')", event.getType().endsWith("expired") ? "CANCELLED" : "FAILED", p.id());
        } else if (event.getType().equals("checkout.session.completed")) {
            db.update("UPDATE payments SET status='PROCESSING', updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='CREATED'", p.id());
        }
    }

    public record Checkout(String url) {}
    private record Order(UUID id, String status, long total, String currency, String email) {}
    private record Attempt(UUID id, String sessionId) {}
    private record Prepared(Order order, Attempt attempt) {}
    private record Settlement(UUID id, long amount, String currency, String status, UUID userId, String reference, String orderStatus) {}
}
