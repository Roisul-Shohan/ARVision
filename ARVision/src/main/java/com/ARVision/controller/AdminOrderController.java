package com.ARVision.controller;

import com.ARVision.dto.order.OrderResponse;
import com.ARVision.dto.order.OrderStatusUpdateRequest;
import com.ARVision.entity.Order;
import com.ARVision.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ORDER_MANAGER')")
public class AdminOrderController {

    private final OrderService orderService;

    // GET /api/admin/orders?keyword=john&status=PENDING&page=0&size=20
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                orderService.getAllOrders(keyword, status, page, size));
    }

    // PATCH /api/admin/orders/5/status
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, request));
    }

    // GET /api/admin/orders/stats
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(orderService.getOrderStats());
    }
}