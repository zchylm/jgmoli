package com.aicyber.jgmoli.payment.controller;

import com.aicyber.jgmoli.payment.service.StripePaymentService;
import com.aicyber.jgmoli.payment.service.StripePaymentSettings;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripePaymentAccessTest {
    @Test void onlineTestCheckoutIsRestrictedToAdministrators() {
        var service = mock(StripePaymentService.class);
        var settings = new StripePaymentSettings(true, "test", false, true, "sk_test_example", "whsec_example", "https://jgmoli.com.au/payment-preview/");
        var controller = new StripePaymentController(service, settings);
        var member = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        var admin = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertFalse(controller.config(null).enabled());
        assertFalse(controller.config(member).enabled());
        assertThrows(ResponseStatusException.class, () -> controller.checkout(member, new StripePaymentController.Request("JGM-20261110-001000")));
        verifyNoInteractions(service);
        assertTrue(controller.config(admin).enabled());
        assertTrue(controller.config(admin).testMode());
    }
    @Test void disabledLiveConfigurationNeverAdvertisesCheckout() {
        var controller = new StripePaymentController(mock(StripePaymentService.class), new StripePaymentSettings(true, "live", false, true, "sk_live_example", "whsec_example", "https://jgmoli.com.au/"));
        assertFalse(controller.config(null).enabled());
    }
}
