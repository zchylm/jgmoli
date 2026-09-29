package com.aicyber.jgmoli.catalog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheActiveGstInclusiveCatalogWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/catalog/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(16))
                .andExpect(jsonPath("$[0].currency").value("AUD"))
                .andExpect(jsonPath("$[0].priceIncludesGst").value(true))
                .andExpect(jsonPath("$[0].variantId").isNotEmpty());
    }

    @Test
    void filtersByCategoryAndSubtype() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "displays")
                        .param("subtype", "Gaming monitors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("displays"))
                .andExpect(jsonPath("$[1].category").value("displays"));
    }
}
