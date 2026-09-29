package com.aicyber.jgmoli.checkout.dto;

import java.util.UUID;

public record CheckoutLineRequest(UUID variantId, int quantity) {
}
