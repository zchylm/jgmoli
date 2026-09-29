CREATE SEQUENCE sales_order_reference_seq START WITH 1001;
CREATE SEQUENCE payment_reference_seq START WITH 1001;

CREATE TABLE sales_orders (
    id UUID PRIMARY KEY,
    order_reference VARCHAR(40) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    source VARCHAR(20) NOT NULL CHECK (source IN ('CART', 'BUY_NOW')),
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_PAYMENT'
        CHECK (status IN ('AWAITING_PAYMENT', 'PAID', 'PAYMENT_FAILED', 'CANCELLED')),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    subtotal_ex_gst_cents BIGINT NOT NULL CHECK (subtotal_ex_gst_cents >= 0),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    delivery_cents BIGINT NOT NULL DEFAULT 0 CHECK (delivery_cents >= 0),
    total_cents BIGINT NOT NULL CHECK (total_cents >= 0),
    customer_email_snapshot VARCHAR(320) NOT NULL,
    checkout_idempotency_key VARCHAR(120) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMPTZ,
    UNIQUE (user_id, checkout_idempotency_key)
);

CREATE INDEX idx_sales_orders_user_created ON sales_orders(user_id, created_at DESC);
CREATE INDEX idx_sales_orders_status_created ON sales_orders(status, created_at DESC);

CREATE TABLE sales_order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL CHECK (line_number > 0),
    product_id UUID NOT NULL REFERENCES products(id),
    variant_id UUID NOT NULL REFERENCES product_variants(id),
    sku_snapshot VARCHAR(80) NOT NULL,
    brand_snapshot VARCHAR(100) NOT NULL,
    product_name_snapshot VARCHAR(180) NOT NULL,
    variant_name_snapshot VARCHAR(120) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_inc_gst_cents BIGINT NOT NULL CHECK (unit_price_inc_gst_cents >= 0),
    unit_price_ex_gst_cents BIGINT NOT NULL CHECK (unit_price_ex_gst_cents >= 0),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    line_total_cents BIGINT NOT NULL CHECK (line_total_cents >= 0),
    UNIQUE (order_id, line_number),
    UNIQUE (order_id, variant_id)
);

CREATE INDEX idx_sales_order_items_order ON sales_order_items(order_id, line_number);

CREATE TABLE sales_order_delivery (
    order_id UUID PRIMARY KEY REFERENCES sales_orders(id) ON DELETE CASCADE,
    recipient_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),
    suburb VARCHAR(120) NOT NULL,
    state VARCHAR(10) NOT NULL,
    postcode VARCHAR(10) NOT NULL,
    country_code CHAR(2) NOT NULL DEFAULT 'AU'
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES sales_orders(id),
    payment_reference VARCHAR(40) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL CHECK (provider IN ('DEMO', 'STRIPE')),
    provider_payment_id VARCHAR(160),
    amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    idempotency_key VARCHAR(120) NOT NULL,
    failure_reason VARCHAR(320),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    UNIQUE (order_id, idempotency_key)
);

CREATE INDEX idx_payments_order_created ON payments(order_id, created_at DESC);
CREATE UNIQUE INDEX idx_payments_one_success_per_order ON payments(order_id) WHERE status = 'SUCCEEDED';
