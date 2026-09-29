package com.aicyber.jgmoli.invoice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record InvoiceBusinessDetails(
        String legalName,
        String tradingName,
        String abn,
        String address,
        String email,
        String phone
) {
    public InvoiceBusinessDetails(
            @Value("${jgmoli.invoice.seller-legal-name}") String legalName,
            @Value("${jgmoli.invoice.seller-trading-name}") String tradingName,
            @Value("${jgmoli.invoice.seller-abn}") String abn,
            @Value("${jgmoli.invoice.seller-address}") String address,
            @Value("${jgmoli.invoice.seller-email}") String email,
            @Value("${jgmoli.invoice.seller-phone}") String phone
    ) {
        this.legalName = legalName;
        this.tradingName = tradingName;
        this.abn = abn;
        this.address = address;
        this.email = email;
        this.phone = phone;
    }
}
