package com.aicyber.jgmoli.auth.repository;

import com.aicyber.jgmoli.auth.model.AccountActionToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountActionTokenRepository {
    private final JdbcTemplate jdbc;

    public AccountActionTokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID replaceActive(UUID userId, String purpose, String tokenHash, OffsetDateTime expiresAt) {
        jdbc.update("""
                UPDATE account_action_tokens
                SET revoked_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND purpose = ? AND consumed_at IS NULL AND revoked_at IS NULL
                """, userId, purpose);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO account_action_tokens (id, user_id, purpose, token_hash, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, userId, purpose, tokenHash, expiresAt);
        return id;
    }

    public Optional<AccountActionToken> findActiveForUpdate(String purpose, String tokenHash) {
        return jdbc.query("""
                SELECT id, user_id, purpose, expires_at
                FROM account_action_tokens
                WHERE purpose = ? AND token_hash = ? AND consumed_at IS NULL AND revoked_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                FOR UPDATE
                """, resultSet -> resultSet.next() ? Optional.of(new AccountActionToken(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("purpose"),
                resultSet.getObject("expires_at", OffsetDateTime.class)
        )) : Optional.empty(), purpose, tokenHash);
    }

    public void consume(UUID id) {
        jdbc.update("""
                UPDATE account_action_tokens SET consumed_at = CURRENT_TIMESTAMP
                WHERE id = ? AND consumed_at IS NULL AND revoked_at IS NULL
                """, id);
    }
}

