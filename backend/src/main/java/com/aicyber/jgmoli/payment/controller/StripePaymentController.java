package com.aicyber.jgmoli.payment.controller;

import com.aicyber.jgmoli.payment.service.StripePaymentService;
import com.aicyber.jgmoli.payment.service.StripePaymentSettings;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments/stripe")
public class StripePaymentController {
    private final StripePaymentService service;
    private final StripePaymentSettings settings;
    public StripePaymentController(StripePaymentService service, StripePaymentSettings settings) { this.service = service; this.settings = settings; }
    private boolean permitted(Authentication auth) {
        return settings.live() || !settings.testAdminOnly() || (auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())));
    }
    @GetMapping("/config")
    public Configuration config(Authentication auth) {
        boolean enabled;
        try { settings.requireConfigured(); enabled = permitted(auth); }
        catch (IllegalStateException e) { enabled = false; }
        return new Configuration(enabled, !settings.live());
    }
    @PostMapping("/checkout")
    public StripePaymentService.Checkout checkout(Authentication auth, @RequestBody Request request) {
        if (!permitted(auth)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payments are not available for this account.");
        return service.create(UUID.fromString(auth.getName()), request.orderReference());
    }
    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        service.webhook(payload, signature);
    }
    public record Request(String orderReference) {}
    public record Configuration(boolean enabled, boolean testMode) {}
}
