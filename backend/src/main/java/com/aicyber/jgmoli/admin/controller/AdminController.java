package com.aicyber.jgmoli.admin.controller;

import com.aicyber.jgmoli.admin.dto.AdminDtos;
import com.aicyber.jgmoli.admin.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/overview")
    public AdminDtos.Overview overview() {
        return adminService.overview();
    }

    @GetMapping("/orders")
    public List<AdminDtos.OrderSummary> orders(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminService.orders(query, status, page, size);
    }

    @GetMapping("/orders/{reference}")
    public AdminDtos.OrderDetail order(@PathVariable String reference) {
        return adminService.order(reference);
    }

    @PatchMapping("/orders/{reference}/fulfillment")
    public AdminDtos.OrderDetail updateFulfilment(
            Authentication authentication,
            @PathVariable String reference,
            @RequestBody AdminDtos.FulfillmentUpdate request
    ) {
        return adminService.updateFulfilment(UUID.fromString(authentication.getName()), reference, request);
    }

    @GetMapping("/products")
    public List<AdminDtos.Product> products() {
        return adminService.products();
    }

    @PatchMapping("/products/{variantId}")
    public AdminDtos.Product updateProduct(
            Authentication authentication,
            @PathVariable UUID variantId,
            @RequestBody AdminDtos.ProductUpdate request
    ) {
        return adminService.updateProduct(UUID.fromString(authentication.getName()), variantId, request);
    }

    @GetMapping("/inventory")
    public List<AdminDtos.Inventory> inventory() {
        return adminService.inventory();
    }

    @GetMapping("/inventory/movements")
    public List<AdminDtos.Movement> movements() {
        return adminService.movements();
    }

    @PostMapping("/inventory/items")
    public AdminDtos.Inventory createInventoryItem(
            Authentication authentication,
            @RequestBody AdminDtos.InventoryItemCreate request
    ) {
        return adminService.createInventoryItem(UUID.fromString(authentication.getName()), request);
    }

    @PatchMapping("/inventory/{itemId}")
    public AdminDtos.Inventory adjustInventory(
            Authentication authentication,
            @PathVariable UUID itemId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AdminDtos.InventoryAdjustment request
    ) {
        return adminService.adjustInventory(UUID.fromString(authentication.getName()), itemId, idempotencyKey, request);
    }
}
