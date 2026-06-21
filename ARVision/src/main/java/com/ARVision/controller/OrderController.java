package com.ARVision.controller;

import com.ARVision.dto.order.*;
import com.ARVision.entity.Order;
import com.ARVision.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    // Check saved address before ordering
    // GET /api/customer/orders/check-address
    @GetMapping("/check-address")
    public ResponseEntity<AddressCheckResponse> checkAddress(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(orderService.checkAddress(email));
    }

    // Place order from cart
    // POST /api/customer/orders/from-cart
    @PostMapping("/from-cart")
    public ResponseEntity<OrderResponse> placeOrderFromCart(
            @AuthenticationPrincipal String email,
            @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrderFromCart(email, request));
    }

    // Place direct order (without cart)
    // POST /api/customer/orders/direct
    @PostMapping("/direct")
    public ResponseEntity<OrderResponse> placeDirectOrder(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody DirectOrderRequest request) {
        return ResponseEntity.ok(orderService.placeDirectOrder(email, request));
    }

    // Get my orders
    // GET /api/customer/orders?page=0&size=10
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getMyOrders(email, page, size));
    }

    // Get single order
    // GET /api/customer/orders/5
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(email, orderId));
    }

    // Cancel order
    // DELETE /api/customer/orders/5/cancel
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(email, orderId));
    }
}