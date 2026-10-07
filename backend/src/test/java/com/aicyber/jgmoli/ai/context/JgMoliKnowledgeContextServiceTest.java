package com.aicyber.jgmoli.ai.context;

import com.aicyber.jgmoli.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JgMoliKnowledgeContextServiceTest {

    @Test
    void describesCurrentBrandArchitectureWithoutPublishingInternalStrategy() {
        CatalogRepository catalogRepository = mock(CatalogRepository.class);
        when(catalogRepository.findActiveProducts(null, null)).thenReturn(List.of());
        JgMoliKnowledgeContextService service = new JgMoliKnowledgeContextService(
                catalogRepository,
                "AI CYBER AUSTRALIA PTY LTD",
                "JG MOLI",
                "22 689 546 450",
                "205 Kensington Rd, West Melbourne VIC 3003",
                "support@jgmoli.com.au",
                "+61 436 365 016"
        );

        String context = service.currentContext();

        assertTrue(context.contains("Complete, Real and Only Here"));
        assertTrue(context.contains("MOLI Cockpit is a personal immersive space"));
        assertTrue(context.contains("MOLI Racer is a dedicated, complete motion-racing machine"));
        assertTrue(context.contains("MOLI Racer is motion-first at every level"));
        assertTrue(context.contains("MOLI Cockpit Ultra adds a full-motion platform"));
        assertTrue(context.contains("Never say that the entire MOLI Cockpit line has no motion"));
        assertTrue(context.contains("JG MOLI Arena turns a familiar room into shared active play"));
        assertTrue(context.contains("interactive light-and-projection system"));
        assertTrue(context.contains("Complete cockpits, complete racers and interactive arenas must not be described as ordinary Sim Gear accessories"));
        assertFalse(context.contains("profit engine"));
        assertFalse(context.contains("CAC"));
    }
}
