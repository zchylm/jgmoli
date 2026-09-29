CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    catalog_variant_id UUID UNIQUE REFERENCES product_variants(id) ON DELETE SET NULL,
    sku VARCHAR(80) NOT NULL UNIQUE,
    brand VARCHAR(100) NOT NULL,
    name VARCHAR(180) NOT NULL,
    item_type VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO inventory_items (id, catalog_variant_id, sku, brand, name, item_type)
SELECT v.id, v.id, v.sku, p.brand, p.name, p.subtype
FROM product_variants v
JOIN products p ON p.id = v.product_id;

ALTER TABLE inventory_balances ADD COLUMN item_id UUID;
UPDATE inventory_balances SET item_id = variant_id;
ALTER TABLE inventory_balances ALTER COLUMN item_id SET NOT NULL;
ALTER TABLE inventory_balances
    ADD CONSTRAINT fk_inventory_balance_item FOREIGN KEY (item_id) REFERENCES inventory_items(id);
ALTER TABLE inventory_balances DROP CONSTRAINT inventory_balances_pkey;
ALTER TABLE inventory_balances ADD PRIMARY KEY (item_id, location_id);
ALTER TABLE inventory_balances ALTER COLUMN variant_id DROP NOT NULL;

ALTER TABLE inventory_movements ADD COLUMN item_id UUID;
UPDATE inventory_movements SET item_id = variant_id;
ALTER TABLE inventory_movements ALTER COLUMN item_id SET NOT NULL;
ALTER TABLE inventory_movements
    ADD CONSTRAINT fk_inventory_movement_item FOREIGN KEY (item_id) REFERENCES inventory_items(id);
ALTER TABLE inventory_movements ALTER COLUMN variant_id DROP NOT NULL;

CREATE INDEX idx_inventory_items_type_name ON inventory_items(item_type, name);
CREATE INDEX idx_inventory_movements_item_created ON inventory_movements(item_id, created_at DESC);
