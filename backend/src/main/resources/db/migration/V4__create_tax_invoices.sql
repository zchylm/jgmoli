CREATE SEQUENCE tax_invoice_reference_seq START WITH 1001;

CREATE TABLE sales_invoices (
    id UUID PRIMARY KEY,
    invoice_number VARCHAR(40) NOT NULL UNIQUE,
    order_id UUID NOT NULL UNIQUE REFERENCES sales_orders(id),
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    document_type VARCHAR(30) NOT NULL DEFAULT 'TAX_INVOICE'
        CHECK (document_type = 'TAX_INVOICE'),
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED'
        CHECK (status IN ('ISSUED', 'VOID')),
    seller_legal_name VARCHAR(180) NOT NULL,
    seller_trading_name VARCHAR(120) NOT NULL,
    seller_abn VARCHAR(20) NOT NULL,
    seller_address VARCHAR(320) NOT NULL,
    seller_email VARCHAR(320) NOT NULL,
    seller_phone VARCHAR(40) NOT NULL,
    buyer_name VARCHAR(160) NOT NULL,
    buyer_email VARCHAR(320) NOT NULL,
    buyer_address_line_1 VARCHAR(200) NOT NULL,
    buyer_address_line_2 VARCHAR(200),
    buyer_suburb VARCHAR(120) NOT NULL,
    buyer_state VARCHAR(10) NOT NULL,
    buyer_postcode VARCHAR(10) NOT NULL,
    buyer_country_code CHAR(2) NOT NULL,
    currency CHAR(3) NOT NULL,
    subtotal_ex_gst_cents BIGINT NOT NULL CHECK (subtotal_ex_gst_cents >= 0),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    delivery_cents BIGINT NOT NULL CHECK (delivery_cents >= 0),
    total_cents BIGINT NOT NULL CHECK (total_cents >= 0),
    amount_paid_cents BIGINT NOT NULL CHECK (amount_paid_cents >= 0),
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_invoices_paid_total_check CHECK (amount_paid_cents = total_cents)
);

CREATE INDEX idx_sales_invoices_issued_at ON sales_invoices(issued_at DESC);
CREATE INDEX idx_sales_invoices_buyer_email ON sales_invoices(LOWER(buyer_email));

CREATE TABLE sales_invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES sales_invoices(id) ON DELETE CASCADE,
    line_number INTEGER NOT NULL CHECK (line_number > 0),
    sku_snapshot VARCHAR(80) NOT NULL,
    description_snapshot VARCHAR(320) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    unit_price_ex_gst_cents BIGINT NOT NULL CHECK (unit_price_ex_gst_cents >= 0),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    line_total_inc_gst_cents BIGINT NOT NULL CHECK (line_total_inc_gst_cents >= 0),
    taxable BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (invoice_id, line_number)
);

CREATE INDEX idx_sales_invoice_lines_invoice ON sales_invoice_lines(invoice_id, line_number);
