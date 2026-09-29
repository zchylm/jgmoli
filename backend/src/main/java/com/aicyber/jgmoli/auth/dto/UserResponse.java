package com.aicyber.jgmoli.auth.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String customerReference,
        String email,
        String displayName,
        String role,
        boolean emailVerified
) {
}
