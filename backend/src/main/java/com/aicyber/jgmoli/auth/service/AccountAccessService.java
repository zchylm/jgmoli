package com.aicyber.jgmoli.auth.service;

import com.aicyber.jgmoli.auth.dto.MessageResponse;
import com.aicyber.jgmoli.auth.model.User;
import com.aicyber.jgmoli.auth.repository.AccountActionTokenRepository;
import com.aicyber.jgmoli.auth.repository.UserRepository;
import com.aicyber.jgmoli.email.service.TransactionalEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountAccessService {
    private static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    private static final String PASSWORD_RESET = "PASSWORD_RESET";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final AccountActionTokenRepository tokens;
    private final TransactionalEmailService emails;
    private final PasswordEncoder passwordEncoder;
    private final long verificationHours;
    private final long resetMinutes;

    public AccountAccessService(
            UserRepository users,
            AccountActionTokenRepository tokens,
            TransactionalEmailService emails,
            PasswordEncoder passwordEncoder,
            @Value("${jgmoli.auth.email-verification-expiration-hours:24}") long verificationHours,
            @Value("${jgmoli.auth.password-reset-expiration-minutes:30}") long resetMinutes
    ) {
        this.users = users;
        this.tokens = tokens;
        this.emails = emails;
        this.passwordEncoder = passwordEncoder;
        this.verificationHours = verificationHours;
        this.resetMinutes = resetMinutes;
    }

    @Transactional
    public void queueVerification(User user) {
        if (user.emailVerifiedAt() != null) return;
        String rawToken = newToken();
        UUID tokenId = tokens.replaceActive(user.id(), EMAIL_VERIFICATION, hash(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(verificationHours));
        emails.queueVerification(user, tokenId, rawToken);
    }

    @Transactional
    public MessageResponse resendVerification(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
        if (user.emailVerifiedAt() != null) return new MessageResponse("Your email is already verified.");
        queueVerification(user);
        return new MessageResponse("A new verification link is on its way.");
    }

    @Transactional
    public MessageResponse confirmVerification(String rawToken) {
        var token = tokens.findActiveForUpdate(EMAIL_VERIFICATION, hash(requireToken(rawToken)))
                .orElseThrow(() -> new IllegalArgumentException("This verification link is invalid or has expired."));
        users.markEmailVerified(token.userId());
        tokens.consume(token.id());
        return new MessageResponse("Your email is verified. Welcome to JG MOLI.");
    }

    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        String normalised = normaliseOptionalEmail(email);
        if (normalised != null) {
            users.findByEmail(normalised)
                    .filter(user -> "ACTIVE".equals(user.status()))
                    .ifPresent(this::queuePasswordReset);
        }
        return new MessageResponse("If an account matches that email, a reset link is on its way.");
    }

    @Transactional
    public MessageResponse confirmPasswordReset(String rawToken, String password) {
        String validPassword = requirePassword(password);
        var token = tokens.findActiveForUpdate(PASSWORD_RESET, hash(requireToken(rawToken)))
                .orElseThrow(() -> new IllegalArgumentException("This password reset link is invalid or has expired."));
        users.updatePassword(token.userId(), passwordEncoder.encode(validPassword));
        tokens.consume(token.id());
        return new MessageResponse("Your password has been updated. Log in to continue.");
    }

    private void queuePasswordReset(User user) {
        String rawToken = newToken();
        UUID tokenId = tokens.replaceActive(user.id(), PASSWORD_RESET, hash(rawToken),
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(resetMinutes));
        emails.queuePasswordReset(user, tokenId, rawToken);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Secure account links are temporarily unavailable.");
        }
    }

    private String requireToken(String token) {
        if (token == null || token.isBlank() || token.length() > 200) {
            throw new IllegalArgumentException("This account link is invalid or has expired.");
        }
        return token.trim();
    }

    private String requirePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 200) {
            throw new IllegalArgumentException("Use between 8 and 200 characters for your password.");
        }
        return password;
    }

    private String normaliseOptionalEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@") || email.length() > 320) return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
