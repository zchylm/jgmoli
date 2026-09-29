package com.aicyber.jgmoli.email.model;

import java.util.UUID;

public record EmailDraft(
        String messageType,
        String recipientEmail,
        String recipientName,
        String subject,
        String textBody,
        String htmlBody,
        String aggregateType,
        UUID aggregateId,
        String idempotencyKey
) {
}

