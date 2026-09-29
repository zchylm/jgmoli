CREATE TABLE transactional_email_outbox (
    id UUID PRIMARY KEY,
    message_type VARCHAR(60) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    recipient_name VARCHAR(160),
    subject VARCHAR(255) NOT NULL,
    text_body TEXT,
    html_body TEXT,
    aggregate_type VARCHAR(60),
    aggregate_id UUID,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempted_at TIMESTAMPTZ,
    provider_message_id VARCHAR(160) UNIQUE,
    last_error VARCHAR(500),
    queued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactional_email_outbox_body_check
        CHECK (text_body IS NOT NULL OR html_body IS NOT NULL),
    CONSTRAINT transactional_email_outbox_status_check
        CHECK (status IN ('QUEUED', 'SENDING', 'RETRY_PENDING', 'ACCEPTED', 'DELIVERED',
                          'DELAYED', 'FAILED', 'BOUNCED', 'COMPLAINED', 'SUPPRESSED')),
    CONSTRAINT transactional_email_outbox_attempt_count_check CHECK (attempt_count >= 0)
);

CREATE INDEX transactional_email_outbox_ready_idx
    ON transactional_email_outbox (next_attempt_at, queued_at)
    WHERE status IN ('QUEUED', 'RETRY_PENDING');

CREATE INDEX transactional_email_outbox_aggregate_idx
    ON transactional_email_outbox (aggregate_type, aggregate_id);

