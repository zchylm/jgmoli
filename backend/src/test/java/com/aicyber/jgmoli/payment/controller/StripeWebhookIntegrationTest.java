package com.aicyber.jgmoli.payment.controller;

import com.jayway.jsonpath.JsonPath;
import com.stripe.Stripe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"jgmoli.stripe.test-enabled=true", "jgmoli.stripe.secret-key=sk_test_example", "jgmoli.stripe.webhook-secret=whsec_test"})
@AutoConfigureMockMvc
@Transactional
class StripeWebhookIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;

    @Test
    void paidWebhookIsIdempotentAndChecksAmountAndSignature() throws Exception {
        String email = "stripe-" + UUID.randomUUID() + "@example.com";
        String auth = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"play-better-2026\",\"displayName\":\"Test\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(auth, "$.accessToken");
        String order = mvc.perform(post("/api/checkout/orders").header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"source":"BUY_NOW","items":[{"variantId":"30000000-0000-0000-0000-000000000001","quantity":1}],"delivery":{"recipientName":"Test","phone":"0412345678","addressLine1":"10 Test Street","suburb":"Melbourne","state":"VIC","postcode":"3000","countryCode":"AU"}}
                    """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String reference = JsonPath.read(order, "$.orderReference");
        UUID orderId = UUID.fromString(JsonPath.read(order, "$.id"));
        UUID paymentId = UUID.randomUUID();
        db.update("INSERT INTO payments(id,order_id,payment_reference,provider,amount_cents,currency,status,idempotency_key,stripe_session_id) VALUES (?,?,?,'STRIPE',29900,'AUD','CREATED',?,?)", paymentId, orderId, "T-" + paymentId, paymentId.toString(), "cs_test_123");
        String wrong = event(reference, paymentId, 1, "paid");
        mvc.perform(post("/api/payments/stripe/webhook").header("Stripe-Signature", sign(wrong)).contentType(MediaType.APPLICATION_JSON).content(wrong)).andExpect(status().isBadRequest());
        String paid = event(reference, paymentId, 29900, "paid");
        mvc.perform(post("/api/payments/stripe/webhook").header("Stripe-Signature", "t=1,v1=bad").contentType(MediaType.APPLICATION_JSON).content(paid)).andExpect(status().isBadRequest());
        assertEquals("AWAITING_PAYMENT", db.queryForObject("SELECT status FROM sales_orders WHERE id=?", String.class, orderId));
        String pending = event(reference, paymentId, 29900, "unpaid");
        mvc.perform(post("/api/payments/stripe/webhook").header("Stripe-Signature", sign(pending)).contentType(MediaType.APPLICATION_JSON).content(pending)).andExpect(status().isOk());
        assertEquals("PROCESSING", db.queryForObject("SELECT status FROM payments WHERE id=?", String.class, paymentId));
        assertEquals("AWAITING_PAYMENT", db.queryForObject("SELECT status FROM sales_orders WHERE id=?", String.class, orderId));
        String failed = pending.replace("checkout.session.completed", "checkout.session.async_payment_failed");
        mvc.perform(post("/api/payments/stripe/webhook").header("Stripe-Signature", sign(failed)).contentType(MediaType.APPLICATION_JSON).content(failed)).andExpect(status().isOk());
        assertEquals("FAILED", db.queryForObject("SELECT status FROM payments WHERE id=?", String.class, paymentId));
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/payments/stripe/webhook").header("Stripe-Signature", sign(paid)).contentType(MediaType.APPLICATION_JSON).content(paid)).andExpect(status().isOk());
        assertEquals("PAID", db.queryForObject("SELECT status FROM sales_orders WHERE id=?", String.class, orderId));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM sales_invoices WHERE order_id=?", Integer.class, orderId));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM payments WHERE order_id=? AND status='SUCCEEDED'", Integer.class, orderId));
    }

    private String event(String reference, UUID id, int amount, String paymentStatus) {
        return """
            {"id":"evt_test_123","object":"event","api_version":"%s","livemode":false,"type":"checkout.session.completed","data":{"object":{"id":"cs_test_123","object":"checkout.session","livemode":false,"client_reference_id":"%s","metadata":{"jgmoli_payment_id":"%s"},"amount_total":%d,"currency":"aud","payment_status":"%s","payment_intent":"pi_test_123"}}}
            """.formatted(Stripe.API_VERSION, reference, id, amount, paymentStatus);
    }
    private String sign(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
    }
}
