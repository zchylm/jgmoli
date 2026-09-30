package com.aicyber.jgmoli.checkout.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAnInvoiceReadyOrderAndCompletesAnIdempotentDemoPayment() throws Exception {
        String token = register();
        String checkoutKey = UUID.randomUUID().toString();
        String request = """
                {
                  "source":"BUY_NOW",
                  "items":[{"variantId":"30000000-0000-0000-0000-000000000001","quantity":1}],
                  "delivery":{
                    "recipientName":"Jordan Player",
                    "phone":"0412 345 678",
                    "addressLine1":"10 Collins Street",
                    "addressLine2":"Level 2",
                    "suburb":"Melbourne",
                    "state":"VIC",
                    "postcode":"3000",
                    "countryCode":"AU"
                  }
                }
                """;

        MvcResult orderResult = mockMvc.perform(post("/api/checkout/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", checkoutKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderReference").value(matchesPattern("JGM-[0-9]{8}-[0-9]{6}")))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andExpect(jsonPath("$.subtotalExGstCents").value(27182))
                .andExpect(jsonPath("$.gstCents").value(2718))
                .andExpect(jsonPath("$.totalCents").value(29900))
                .andExpect(jsonPath("$.items[0].sku").value("DEMO-LG-27GS60F"))
                .andExpect(jsonPath("$.delivery.state").value("VIC"))
                .andReturn();
        String orderReference = JsonPath.read(orderResult.getResponse().getContentAsString(), "$.orderReference");

        mockMvc.perform(post("/api/checkout/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", checkoutKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderReference").value(orderReference));

        String paymentKey = UUID.randomUUID().toString();
        String paymentBody = "{\"orderReference\":\"%s\"}".formatted(orderReference);
        MvcResult paymentResult = mockMvc.perform(post("/api/payments/demo")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", paymentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value(matchesPattern("JGM-PAY-[0-9]{8}-[0-9]{6}")))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.orderStatus").value("PAID"))
                .andReturn();
        String paymentReference = JsonPath.read(paymentResult.getResponse().getContentAsString(), "$.paymentReference");

        mockMvc.perform(post("/api/payments/demo")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", paymentKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentReference").value(paymentReference));

        mockMvc.perform(get("/api/invoices/orders/{orderReference}", orderReference)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(matchesPattern("JGM-INV-[0-9]{4}-[0-9]{6}")))
                .andExpect(jsonPath("$.documentType").value("TAX_INVOICE"))
                .andExpect(jsonPath("$.sellerLegalName").value("AI CYBER AUSTRALIA PTY LTD"))
                .andExpect(jsonPath("$.sellerAbn").value("22 689 546 450"))
                .andExpect(jsonPath("$.sellerAddress").value("205 Kensington Rd, West Melbourne VIC 3003, Australia"))
                .andExpect(jsonPath("$.buyerName").value("Jordan Player"))
                .andExpect(jsonPath("$.buyerAddress.postcode").value("3000"))
                .andExpect(jsonPath("$.subtotalExGstCents").value(27182))
                .andExpect(jsonPath("$.gstCents").value(2718))
                .andExpect(jsonPath("$.totalCents").value(29900))
                .andExpect(jsonPath("$.amountPaidCents").value(29900))
                .andExpect(jsonPath("$.lines[0].sku").value("DEMO-LG-27GS60F"))
                .andExpect(jsonPath("$.lines[0].taxable").value(true));

        mockMvc.perform(get("/api/orders/{orderReference}", orderReference)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderReference").value(orderReference))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].items[0].brand").value("LG"))
                .andExpect(jsonPath("$[0].items[0].productName").value("UltraGear 27GS60F 27-inch 180Hz Monitor"));

        Integer orderEmails = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM transactional_email_outbox email
                JOIN sales_orders orders ON orders.id = email.aggregate_id
                WHERE email.aggregate_type = 'ORDER'
                  AND email.message_type = 'ORDER_CREATED'
                  AND orders.order_reference = ?
                """, Integer.class, orderReference);
        Integer invoiceEmails = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM transactional_email_outbox email
                JOIN sales_invoices invoices ON invoices.id = email.aggregate_id
                JOIN sales_orders orders ON orders.id = invoices.order_id
                WHERE email.aggregate_type = 'INVOICE'
                  AND email.message_type = 'INVOICE_ISSUED'
                  AND orders.order_reference = ?
                """, Integer.class, orderReference);
        assertEquals(1, orderEmails);
        assertEquals(1, invoiceEmails);
    }

    @Test
    void rejectsInvalidDeliveryDetailsBeforeCreatingAnOrder() throws Exception {
        mockMvc.perform(post("/api/checkout/orders")
                        .header("Authorization", "Bearer " + register())
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source":"CART",
                                  "items":[{"variantId":"30000000-0000-0000-0000-000000000001","quantity":1}],
                                  "delivery":{"recipientName":"Jordan","phone":"123","addressLine1":"10 Test Street","suburb":"Melbourne","state":"VIC","postcode":"30","countryCode":"AU"}
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"));
    }

    private String register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"checkout-%s@example.com","password":"play-better-2026","displayName":"Jordan"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
