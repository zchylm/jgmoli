package com.aicyber.jgmoli.admin.controller;

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
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void protectsAdminRoutesAndRecordsIdempotentInventoryChanges() throws Exception {
        String email = "admin-%s@example.com".formatted(UUID.randomUUID());
        String customerToken = register(email);

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        jdbcTemplate.update("UPDATE users SET role = 'ADMIN', auth_version = auth_version + 1 WHERE email = ?", email);
        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized());

        String adminToken = login(email);
        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersToday").isNumber())
                .andExpect(jsonPath("$.lowStockProducts").isNumber());

        String key = UUID.randomUUID().toString();
        String adjustment = """
                {"movementType":"RECEIPT","quantityDelta":5,"reason":"Initial demonstration receipt","reorderLevel":2}
                """;
        mockMvc.perform(patch("/api/admin/inventory/30000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(5))
                .andExpect(jsonPath("$.available").value(5))
                .andExpect(jsonPath("$.reorderLevel").value(2));

        mockMvc.perform(patch("/api/admin/inventory/30000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onHand").value(5));

        mockMvc.perform(get("/api/admin/inventory/movements").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType").value("RECEIPT"));

        mockMvc.perform(post("/api/admin/inventory/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"WAREHOUSE-DEMO-001",
                                  "brand":"Demo Gear",
                                  "productName":"Warehouse-only Racing Wheel",
                                  "categoryCode":"sim",
                                  "productType":"Racing wheels",
                                  "initialQuantity":3,
                                  "reorderLevel":1,
                                  "reason":"Opening warehouse count"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("WAREHOUSE-DEMO-001"))
                .andExpect(jsonPath("$.linkedToCatalog").value(true))
                .andExpect(jsonPath("$.onHand").value(3));

        Integer catalogueProducts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_variants WHERE sku = 'WAREHOUSE-DEMO-001'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, catalogueProducts);
        Map<String, Object> createdProduct = jdbcTemplate.queryForMap("""
                SELECT p.status, v.active, v.price_cents
                FROM products p JOIN product_variants v ON v.product_id = p.id
                WHERE v.sku = 'WAREHOUSE-DEMO-001'
                """);
        org.junit.jupiter.api.Assertions.assertEquals("DRAFT", createdProduct.get("status"));
        org.junit.jupiter.api.Assertions.assertEquals(false, createdProduct.get("active"));
        org.junit.jupiter.api.Assertions.assertEquals(0, ((Number) createdProduct.get("price_cents")).intValue());
    }

    @Test
    void updatesProductPriceAndWritesAnAuditEvent() throws Exception {
        String email = "catalog-admin-%s@example.com".formatted(UUID.randomUUID());
        register(email);
        jdbcTemplate.update("UPDATE users SET role = 'ADMIN', auth_version = auth_version + 1 WHERE email = ?", email);
        String token = login(email);

        mockMvc.perform(patch("/api/admin/products/30000000-0000-0000-0000-000000000002")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priceCents\":129500,\"status\":\"ACTIVE\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(129500));

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_events WHERE action = 'PRODUCT_UPDATED'", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(1, audits);
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"play-better-2026","displayName":"Admin Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"play-better-2026"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
