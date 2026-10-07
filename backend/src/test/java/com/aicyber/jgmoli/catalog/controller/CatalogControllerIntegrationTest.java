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
                .andExpect(jsonPath("$.length()").value(25))
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

    @Test
    void doesNotReturnTheArchivedLegacyCockpit() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "sim")
                        .param("subtype", "Cockpits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returnsTheCockpitCollectionAtTheAustralianRrp() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "sim")
                        .param("subtype", "Complete cockpits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].name").value("MOLI Cockpit"))
                .andExpect(jsonPath("$[0].priceCents").value(1199900))
                .andExpect(jsonPath("$[1].priceCents").value(2499900))
                .andExpect(jsonPath("$[2].priceCents").value(2999900))
                .andExpect(jsonPath("$[3].priceCents").value(3999900));
    }

    @Test
    void returnsTheRacerCollectionAtTheAustralianRrp() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "sim")
                        .param("subtype", "Complete racers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("MOLI Racer Core"))
                .andExpect(jsonPath("$[0].priceCents").value(2690000))
                .andExpect(jsonPath("$[1].name").value("MOLI Racer Signature"))
                .andExpect(jsonPath("$[1].priceCents").value(3490000))
                .andExpect(jsonPath("$[2].name").value("MOLI Racer Elite"))
                .andExpect(jsonPath("$[2].priceCents").value(4490000));
    }

    @Test
    void returnsTheArenaCollectionAtTheAustralianRrp() throws Exception {
        mockMvc.perform(get("/api/catalog/products")
                        .param("category", "sim")
                        .param("subtype", "Interactive arenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("JG MOLI Arena Core"))
                .andExpect(jsonPath("$[0].priceCents").value(999900))
                .andExpect(jsonPath("$[1].name").value("JG MOLI Arena Signature"))
                .andExpect(jsonPath("$[1].priceCents").value(1399900))
                .andExpect(jsonPath("$[2].name").value("JG MOLI Arena Venue"))
                .andExpect(jsonPath("$[2].priceCents").value(1799900));
    }
}
