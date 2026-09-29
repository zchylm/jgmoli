UPDATE inventory_items item
SET catalog_variant_id = variant.id,
    updated_at = CURRENT_TIMESTAMP
FROM product_variants variant
WHERE item.sku = variant.sku
  AND item.catalog_variant_id IS NULL;

ALTER TABLE inventory_items ALTER COLUMN catalog_variant_id SET NOT NULL;
