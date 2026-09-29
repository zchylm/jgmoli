package com.aicyber.jgmoli.email.model;

import java.util.UUID;

public record QueuedEmail(
        UUID id,
        String messageType,
        String recipientEmail,
        String recipientName,
        String subject,
        String textBody,
        String htmlBody,
        String idempotencyKey,
        int attemptCount
) {
}

