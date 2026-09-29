package com.aicyber.jgmoli.payment.controller;

import com.aicyber.jgmoli.payment.dto.DemoPaymentRequest;
import com.aicyber.jgmoli.payment.dto.PaymentResponse;
import com.aicyber.jgmoli.payment.service.DemoPaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/demo")
@ConditionalOnProperty(name = "jgmoli.payment.demo-enabled", havingValue = "true")
public class DemoPaymentController {

    private final DemoPaymentService demoPaymentService;

    public DemoPaymentController(DemoPaymentService demoPaymentService) {
        this.demoPaymentService = demoPaymentService;
    }

    @PostMapping
    public PaymentResponse complete(
            Authentication authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody DemoPaymentRequest request
    ) {
        return demoPaymentService.complete(
                UUID.fromString(authentication.getName()), idempotencyKey,
                request == null ? null : request.orderReference());
    }
}
