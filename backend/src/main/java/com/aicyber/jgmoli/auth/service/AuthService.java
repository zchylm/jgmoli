package com.aicyber.jgmoli.auth.service;

import com.aicyber.jgmoli.auth.dto.AuthResponse;
import com.aicyber.jgmoli.auth.dto.LoginRequest;
import com.aicyber.jgmoli.auth.dto.RegisterRequest;
import com.aicyber.jgmoli.auth.dto.UserResponse;
import com.aicyber.jgmoli.auth.model.User;
import com.aicyber.jgmoli.auth.repository.UserRepository;
import com.aicyber.jgmoli.auth.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountAccessService accountAccessService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       AccountAccessService accountAccessService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accountAccessService = accountAccessService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Enter your account details.");
        }
        String email = normaliseEmail(request.email());
        String password = requirePassword(request.password());
        String displayName = requireDisplayName(request.displayName());
        if (userRepository.findByEmail(email).isPresent()) {
            throw duplicateEmail();
        }

        UUID id = UUID.randomUUID();
        try {
            User user = userRepository.create(
                    id,
                    customerReference(id),
                    email,
                    passwordEncoder.encode(password),
                    displayName,
                    null
            );
            accountAccessService.queueVerification(user);
            return responseFor(user);
        } catch (DuplicateKeyException exception) {
            throw duplicateEmail();
        }
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw invalidCredentials();
        }
        String email = normaliseEmail(request.email());
        String password = requireLoginPassword(request.password());
        User user = userRepository.findByEmail(email).orElseThrow(this::invalidCredentials);
        if (!"ACTIVE".equals(user.status()) || !passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return responseFor(user);
    }

    public UserResponse currentUser(UUID userId) {
        return userRepository.findById(userId)
                .map(this::toUserResponse)
                .orElseThrow(() -> new IllegalArgumentException("User account not found."));
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(
                jwtService.createToken(user.id(), user.email(), user.role(), user.authVersion()),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.id(),
                user.customerReference(),
                user.email(),
                user.displayName(),
                user.role(),
                user.emailVerifiedAt() != null
        );
    }

    private String normaliseEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@") || email.length() > 320) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requirePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 200) {
            throw new IllegalArgumentException("Use between 8 and 200 characters for your password.");
        }
        return password;
    }

    private String requireLoginPassword(String password) {
        if (password == null || password.isBlank()) {
            throw invalidCredentials();
        }
        return password;
    }

    private String requireDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new IllegalArgumentException("Enter a name of 120 characters or fewer.");
        }
        return displayName.trim();
    }

    private String customerReference(UUID id) {
        return "JGM-CUS-" + id.toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private IllegalArgumentException duplicateEmail() {
        return new IllegalArgumentException("An account with this email already exists. Log in to continue.");
    }

    private IllegalArgumentException invalidCredentials() {
        return new IllegalArgumentException("Email or password is incorrect.");
    }
}
