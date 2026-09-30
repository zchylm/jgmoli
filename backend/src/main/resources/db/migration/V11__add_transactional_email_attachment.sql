ALTER TABLE transactional_email_outbox
    ADD COLUMN attachment_filename VARCHAR(255),
    ADD COLUMN attachment_content_type VARCHAR(120),
    ADD COLUMN attachment_content_base64 TEXT;

ALTER TABLE transactional_email_outbox
    ADD CONSTRAINT transactional_email_outbox_attachment_check CHECK (
        (attachment_filename IS NULL AND attachment_content_type IS NULL AND attachment_content_base64 IS NULL)
        OR
        (attachment_filename IS NOT NULL AND attachment_content_type IS NOT NULL AND attachment_content_base64 IS NOT NULL)
    );
