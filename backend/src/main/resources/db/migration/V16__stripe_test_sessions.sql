ALTER TABLE payments ADD COLUMN stripe_session_id VARCHAR(160) UNIQUE;
CREATE UNIQUE INDEX payments_one_open_stripe_per_order ON payments(order_id)
    WHERE provider = 'STRIPE' AND status IN ('CREATED', 'PROCESSING');
