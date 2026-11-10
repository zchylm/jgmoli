package com.aicyber.jgmoli.payment.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StripePaymentSettingsTest {
    private StripePaymentSettings settings(String mode, boolean allowLive, String key, String url) {
        return new StripePaymentSettings(true, mode, allowLive, true, key, "whsec_example", url);
    }
    @Test void liveRequiresIndependentEnablementAndMatchingKey() {
        assertThrows(IllegalStateException.class, () -> settings("live", false, "sk_live_example", "https://jgmoli.com.au/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("live", true, "sk_test_example", "https://jgmoli.com.au/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("test", true, "sk_live_example", "https://jgmoli.com.au/").requireConfigured());
        assertDoesNotThrow(() -> settings("live", true, "sk_live_example", "https://jgmoli.com.au/").requireConfigured());
    }
    @Test void restrictedKeysRespectEnvironmentAndLiveEnablement() {
        assertDoesNotThrow(() -> settings("test", false, "rk_test_example", "http://localhost:5173/").requireConfigured());
        assertDoesNotThrow(() -> settings("live", true, "rk_live_example", "https://jgmoli.com.au/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("live", false, "rk_live_example", "https://jgmoli.com.au/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("live", true, "rk_test_example", "https://jgmoli.com.au/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("test", false, "rk_live_example", "https://jgmoli.com.au/").requireConfigured());
    }
    @Test void publicAndUnknownKeysAreRejected() {
        for (String key : new String[] {"pk_test_example", "pk_live_example", "rk_example", "invalid_key", ""}) {
            assertThrows(IllegalStateException.class, () -> settings("test", false, key, "https://jgmoli.com.au/").requireConfigured());
            assertThrows(IllegalStateException.class, () -> settings("live", true, key, "https://jgmoli.com.au/").requireConfigured());
        }
    }
    @Test void liveRequiresHttpsAndCleanReturnUrl() {
        assertThrows(IllegalStateException.class, () -> settings("live", true, "sk_live_example", "http://localhost:5173/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("test", false, "sk_test_example", "http://example.com/").requireConfigured());
        assertThrows(IllegalStateException.class, () -> settings("test", false, "sk_test_example", "https://jgmoli.com.au/?order=other").requireConfigured());
        assertDoesNotThrow(() -> settings("test", false, "sk_test_example", "http://localhost:5173/").requireConfigured());
    }
    @Test void diagnosticsNeverIncludeSecrets() {
        assertFalse(settings("test", false, "sk_test_example", "https://jgmoli.com.au/").toString().contains("sk_test_"));
        assertFalse(settings("test", false, "rk_test_example", "https://jgmoli.com.au/").toString().contains("rk_test_"));
        assertFalse(settings("live", true, "rk_live_example", "https://jgmoli.com.au/").toString().contains("rk_live_"));
    }
}
