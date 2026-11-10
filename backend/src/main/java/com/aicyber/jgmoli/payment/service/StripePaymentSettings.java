package com.aicyber.jgmoli.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;

@Component
public record StripePaymentSettings(
        @Value("${jgmoli.stripe.enabled:false}") boolean enabled,
        @Value("${jgmoli.stripe.mode:test}") String mode,
        @Value("${jgmoli.stripe.allow-live-payments:false}") boolean allowLive,
        @Value("${jgmoli.stripe.test-admin-only:false}") boolean testAdminOnly,
        @Value("${jgmoli.stripe.secret-key:}") String secretKey,
        @Value("${jgmoli.stripe.webhook-secret:}") String webhookSecret,
        @Value("${jgmoli.stripe.return-url:http://localhost:5173/}") String returnUrl
) {
    @Override
    public String toString() { return "StripePaymentSettings[mode=" + mode + ", enabled=" + enabled + "]"; }
    public boolean live() { return "live".equals(mode); }

    public void requireConfigured() {
        if (!enabled || !("test".equals(mode) || "live".equals(mode))) throw new IllegalStateException("Payments are currently unavailable.");
        if (live() && !allowLive) throw new IllegalStateException("Live payments are disabled.");
        if (!secretKey.startsWith(live() ? "sk_live_" : "sk_test_") || !webhookSecret.startsWith("whsec_")) {
            throw new IllegalStateException("Payment configuration is incomplete.");
        }
        URI uri;
        try { uri = URI.create(returnUrl); } catch (IllegalArgumentException e) { throw new IllegalStateException("Payment return URL is invalid."); }
        boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (uri.getHost() == null || !("https".equals(uri.getScheme()) || (!live() && local && "http".equals(uri.getScheme()))) || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getUserInfo() != null) {
            throw new IllegalStateException("Payment return URL is invalid.");
        }
    }
}
