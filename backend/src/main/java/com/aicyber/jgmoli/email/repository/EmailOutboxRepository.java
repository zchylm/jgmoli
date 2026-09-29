package com.aicyber.jgmoli.email.repository;

import com.aicyber.jgmoli.email.model.EmailDraft;
import com.aicyber.jgmoli.email.model.QueuedEmail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailOutboxRepository {
    private final JdbcTemplate jdbc;

    public EmailOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID enqueue(EmailDraft draft) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO transactional_email_outbox
                    (id, message_type, recipient_email, recipient_name, subject, text_body, html_body,
                     aggregate_type, aggregate_id, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idempotency_key) DO NOTHING
                """, id, draft.messageType(), draft.recipientEmail(), draft.recipientName(), draft.subject(),
                draft.textBody(), draft.htmlBody(), draft.aggregateType(), draft.aggregateId(), draft.idempotencyKey());
        return jdbc.queryForObject(
                "SELECT id FROM transactional_email_outbox WHERE idempotency_key = ?",
                (resultSet, row) -> resultSet.getObject(1, UUID.class), draft.idempotencyKey()
        );
    }

    public List<UUID> readyIds(int limit) {
        return jdbc.query("""
                SELECT id FROM transactional_email_outbox
                WHERE ((status IN ('QUEUED', 'RETRY_PENDING') AND next_attempt_at <= CURRENT_TIMESTAMP)
                       OR (status = 'SENDING' AND last_attempted_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes'))
                  AND attempt_count < 5
                ORDER BY next_attempt_at, queued_at
                LIMIT ?
                """, (resultSet, row) -> resultSet.getObject(1, UUID.class), limit);
    }

    public boolean claim(UUID id) {
        return jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'SENDING', attempt_count = attempt_count + 1,
                    last_attempted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, last_error = NULL
                WHERE id = ? AND attempt_count < 5
                  AND ((status IN ('QUEUED', 'RETRY_PENDING') AND next_attempt_at <= CURRENT_TIMESTAMP)
                       OR (status = 'SENDING' AND last_attempted_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes'))
                """, id) == 1;
    }

    public Optional<QueuedEmail> findForDelivery(UUID id) {
        return jdbc.query("""
                SELECT id, message_type, recipient_email, recipient_name, subject, text_body, html_body,
                       idempotency_key, attempt_count
                FROM transactional_email_outbox WHERE id = ? AND status = 'SENDING'
                """, resultSet -> resultSet.next() ? Optional.of(new QueuedEmail(
                resultSet.getObject("id", UUID.class), resultSet.getString("message_type"),
                resultSet.getString("recipient_email"), resultSet.getString("recipient_name"),
                resultSet.getString("subject"), resultSet.getString("text_body"),
                resultSet.getString("html_body"), resultSet.getString("idempotency_key"),
                resultSet.getInt("attempt_count")
        )) : Optional.empty(), id);
    }

    public void markAccepted(UUID id, String providerMessageId) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'ACCEPTED', provider_message_id = ?, accepted_at = CURRENT_TIMESTAMP,
                    text_body = NULL, html_body = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, providerMessageId, id);
    }

    public void markRetryPending(UUID id, String error, OffsetDateTime nextAttemptAt) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'RETRY_PENDING', last_error = ?, next_attempt_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, error, nextAttemptAt, id);
    }

    public void markTerminalFailure(UUID id, String error) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'FAILED', last_error = ?, text_body = NULL, html_body = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, error, id);
    }

    public String providerMessageId(UUID id) {
        return jdbc.query("SELECT provider_message_id FROM transactional_email_outbox WHERE id = ?",
                resultSet -> resultSet.next() ? resultSet.getString(1) : null, id);
    }
}

