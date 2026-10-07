UPDATE products
SET status = 'ARCHIVED', updated_at = CURRENT_TIMESTAMP
WHERE slug = 'playseat-evolution-actifit';

UPDATE product_variants
SET active = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE product_id = (
    SELECT id
    FROM products
    WHERE slug = 'playseat-evolution-actifit'
);
