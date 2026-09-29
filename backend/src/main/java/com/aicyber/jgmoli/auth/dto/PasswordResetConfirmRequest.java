package com.aicyber.jgmoli.auth.dto;

public record PasswordResetConfirmRequest(String token, String password) {
}

