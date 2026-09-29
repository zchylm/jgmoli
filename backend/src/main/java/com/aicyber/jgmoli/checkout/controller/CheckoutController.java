package com.aicyber.jgmoli.checkout.controller;

import com.aicyber.jgmoli.checkout.dto.CreateOrderRequest;
import com.aicyber.jgmoli.checkout.dto.OrderResponse;
import com.aicyber.jgmoli.checkout.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateOrderRequest request
    ) {
        return checkoutService.createOrder(UUID.fromString(authentication.getName()), idempotencyKey, request);
    }

    @GetMapping("/orders/{orderReference}")
    public OrderResponse getOrder(Authentication authentication, @PathVariable String orderReference) {
        return checkoutService.getOrder(UUID.fromString(authentication.getName()), orderReference);
    }

    @GetMapping("/orders")
    public List<OrderResponse> getOrders(Authentication authentication) {
        return checkoutService.getOrders(UUID.fromString(authentication.getName()));
    }
}
