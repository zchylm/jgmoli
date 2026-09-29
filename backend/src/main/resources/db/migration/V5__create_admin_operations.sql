ALTER TABLE sales_orders
    ADD COLUMN fulfillment_status VARCHAR(30) NOT NULL DEFAULT 'UNFULFILLED'
        CHECK (fulfillment_status IN ('UNFULFILLED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    ADD COLUMN carrier VARCHAR(80),
    ADD COLUMN tracking_number VARCHAR(120),
    ADD COLUMN shipped_at TIMESTAMPTZ,
    ADD COLUMN delivered_at TIMESTAMPTZ;

CREATE INDEX idx_sales_orders_fulfillment_created
    ON sales_orders(fulfillment_status, created_at DESC);

CREATE TABLE inventory_locations (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_balances (
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    location_id UUID NOT NULL REFERENCES inventory_locations(id),
    on_hand INTEGER NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0 AND reserved <= on_hand),
    reorder_level INTEGER NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (variant_id, location_id)
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    location_id UUID NOT NULL REFERENCES inventory_locations(id),
    movement_type VARCHAR(30) NOT NULL
        CHECK (movement_type IN ('RECEIPT', 'ADJUSTMENT', 'RESERVATION', 'RELEASE', 'SALE', 'RETURN')),
    on_hand_delta INTEGER NOT NULL DEFAULT 0,
    reserved_delta INTEGER NOT NULL DEFAULT 0,
    reason VARCHAR(240) NOT NULL,
    reference_type VARCHAR(40),
    reference_id VARCHAR(120),
    idempotency_key VARCHAR(120),
    performed_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (performed_by, idempotency_key)
);

CREATE INDEX idx_inventory_movements_variant_created
    ON inventory_movements(variant_id, created_at DESC);

CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id VARCHAR(120) NOT NULL,
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_created ON admin_audit_events(created_at DESC);
CREATE INDEX idx_admin_audit_entity ON admin_audit_events(entity_type, entity_id, created_at DESC);

INSERT INTO inventory_locations (id, code, name)
VALUES ('40000000-0000-0000-0000-000000000001', 'MELBOURNE', 'Melbourne Warehouse');

INSERT INTO inventory_balances (variant_id, location_id)
SELECT id, '40000000-0000-0000-0000-000000000001'
FROM product_variants;
