package com.aicyber.jgmoli.catalog.controller;

import com.aicyber.jgmoli.catalog.dto.CatalogProductResponse;
import com.aicyber.jgmoli.catalog.repository.CatalogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogRepository catalogRepository;

    public CatalogController(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @GetMapping("/products")
    public List<CatalogProductResponse> products(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subtype
    ) {
        String normalisedCategory = category == null || category.isBlank()
                ? null
                : category.trim().toLowerCase(Locale.ROOT);
        String normalisedSubtype = subtype == null || subtype.isBlank() ? null : subtype.trim();
        return catalogRepository.findActiveProducts(normalisedCategory, normalisedSubtype);
    }
}
