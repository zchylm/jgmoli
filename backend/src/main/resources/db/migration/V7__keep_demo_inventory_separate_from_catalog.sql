-- Demo warehouse records remain operationally separate until real stock is imported.
-- A deliberate catalog link can be created later without changing stock history.
UPDATE inventory_items SET catalog_variant_id = NULL;
