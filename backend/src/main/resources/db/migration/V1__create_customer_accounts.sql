CREATE TABLE users (
    id UUID PRIMARY KEY,
    customer_reference VARCHAR(24) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMPTZ,
    auth_version INTEGER NOT NULL DEFAULT 1 CHECK (auth_version > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_customer_reference_format_check
        CHECK (customer_reference ~ '^JGM-CUS-[0-9A-F]{12}$'),
    CONSTRAINT users_email_normalised_check CHECK (email = LOWER(email)),
    CONSTRAINT users_role_check CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX users_email_idx ON users (email);

CREATE TABLE account_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(40) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT account_action_tokens_purpose_check
        CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT account_action_tokens_expiry_check CHECK (expires_at > created_at),
    CONSTRAINT account_action_tokens_completion_check
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE UNIQUE INDEX account_action_tokens_one_active_idx
    ON account_action_tokens (user_id, purpose)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;

CREATE INDEX account_action_tokens_expiry_idx
    ON account_action_tokens (expires_at)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
