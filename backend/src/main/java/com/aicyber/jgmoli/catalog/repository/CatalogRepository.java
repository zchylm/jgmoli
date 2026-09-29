package com.aicyber.jgmoli.catalog.repository;

import com.aicyber.jgmoli.catalog.dto.CatalogProductResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CatalogProductResponse> findActiveProducts(String category, String subtype) {
        return jdbcTemplate.query("""
                SELECT p.id, v.id AS variant_id, p.slug, v.sku, c.code AS category,
                       c.name AS category_name, p.subtype, p.brand, p.name,
                       p.short_description, p.image_key, v.price_cents, v.currency,
                       v.price_includes_gst
                FROM products p
                JOIN product_categories c ON c.id = p.category_id
                JOIN product_variants v ON v.product_id = p.id AND v.active = TRUE
                WHERE p.status = 'ACTIVE'
                  AND (CAST(? AS VARCHAR) IS NULL OR c.code = ?)
                  AND (CAST(? AS VARCHAR) IS NULL OR p.subtype = ?)
                ORDER BY c.sort_order, p.sort_order, p.name
                """, (resultSet, rowNumber) -> new CatalogProductResponse(
                resultSet.getObject("id", java.util.UUID.class),
                resultSet.getObject("variant_id", java.util.UUID.class),
                resultSet.getString("slug"),
                resultSet.getString("sku"),
                resultSet.getString("category"),
                resultSet.getString("category_name"),
                resultSet.getString("subtype"),
                resultSet.getString("brand"),
                resultSet.getString("name"),
                resultSet.getString("short_description"),
                resultSet.getString("image_key"),
                resultSet.getInt("price_cents"),
                resultSet.getString("currency"),
                resultSet.getBoolean("price_includes_gst")
        ), category, category, subtype, subtype);
    }
}
