package com.aicyber.jgmoli.auth.controller;

import com.aicyber.jgmoli.auth.dto.AuthResponse;
import com.aicyber.jgmoli.auth.dto.ActionTokenRequest;
import com.aicyber.jgmoli.auth.dto.LoginRequest;
import com.aicyber.jgmoli.auth.dto.MessageResponse;
import com.aicyber.jgmoli.auth.dto.PasswordResetConfirmRequest;
import com.aicyber.jgmoli.auth.dto.PasswordResetRequest;
import com.aicyber.jgmoli.auth.dto.RegisterRequest;
import com.aicyber.jgmoli.auth.dto.UserResponse;
import com.aicyber.jgmoli.auth.service.AuthService;
import com.aicyber.jgmoli.auth.service.AccountAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountAccessService accountAccessService;

    public AuthController(AuthService authService, AccountAccessService accountAccessService) {
        this.authService = authService;
        this.accountAccessService = accountAccessService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser(Authentication authentication) {
        return authService.currentUser(UUID.fromString(authentication.getName()));
    }

    @PostMapping("/email/verify")
    public MessageResponse verifyEmail(@RequestBody ActionTokenRequest request) {
        return accountAccessService.confirmVerification(request == null ? null : request.token());
    }

    @PostMapping("/email/resend")
    public MessageResponse resendVerification(Authentication authentication) {
        return accountAccessService.resendVerification(UUID.fromString(authentication.getName()));
    }

    @PostMapping("/password/reset/request")
    public MessageResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
        return accountAccessService.requestPasswordReset(request == null ? null : request.email());
    }

    @PostMapping("/password/reset/confirm")
    public MessageResponse confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        return accountAccessService.confirmPasswordReset(
                request == null ? null : request.token(), request == null ? null : request.password());
    }
}
