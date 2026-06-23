package com.ARVision.controller;

import com.ARVision.dto.order.*;
import com.ARVision.dto.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<AddressCheckResponse>> checkAddress(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.checkAddress(email),
                "Address check completed"));
    }

    // Place order from cart
    // POST /api/customer/orders/from-cart
    @PostMapping("/from-cart")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrderFromCart(
            @AuthenticationPrincipal String email,
            @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.placeOrderFromCart(email, request),
                "Order placed successfully"));
    }

    // Place direct order (without cart)
    // POST /api/customer/orders/direct
    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<OrderResponse>> placeDirectOrder(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody DirectOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.placeDirectOrder(email, request),
                "Order placed successfully"));
    }

    // Get my orders
    // GET /api/customer/orders?page=0&size=10
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getMyOrders(email, page, size),
                "Orders fetched successfully"));
    }

    // Get single order
    // GET /api/customer/orders/5
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderById(email, orderId),
                "Order fetched successfully"));
    }

    // Cancel order
    // DELETE /api/customer/orders/5/cancel
    @DeleteMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.cancelOrder(email, orderId),
                "Order cancelled successfully"));
    }
}