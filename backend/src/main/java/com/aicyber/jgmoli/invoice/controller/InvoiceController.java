package com.aicyber.jgmoli.invoice.controller;

import com.aicyber.jgmoli.invoice.dto.InvoiceResponse;
import com.aicyber.jgmoli.invoice.service.InvoiceService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping("/orders/{orderReference}")
    public InvoiceResponse forOrder(Authentication authentication, @PathVariable String orderReference) {
        return invoiceService.invoiceForOrder(UUID.fromString(authentication.getName()), orderReference);
    }
}
