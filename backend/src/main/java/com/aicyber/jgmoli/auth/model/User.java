package com.aicyber.jgmoli.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String customerReference,
        String email,
        String passwordHash,
        String displayName,
        String role,
        String status,
        OffsetDateTime emailVerifiedAt,
        int authVersion,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
