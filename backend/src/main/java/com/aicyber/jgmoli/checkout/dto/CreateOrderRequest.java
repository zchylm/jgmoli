package com.aicyber.jgmoli.checkout.dto;

import java.util.List;

public record CreateOrderRequest(String source, List<CheckoutLineRequest> items, DeliveryRequest delivery) {
}
