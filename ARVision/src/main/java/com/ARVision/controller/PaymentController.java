package com.ARVision.controller;

import com.ARVision.dto.payment.*;
import com.ARVision.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final PaymentService paymentService;

    // Create payment intent — frontend uses clientSecret to show Stripe form
    // POST /api/customer/payments/1/create-intent
    @PostMapping("/{orderId}/create-intent")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) throws StripeException {

        return ResponseEntity.ok(
                paymentService.createPaymentIntent(email, orderId));
    }

    // Get payment receipt
    // GET /api/customer/payments/1/receipt
    @GetMapping("/{orderId}/receipt")
    public ResponseEntity<PaymentResponse> getReceipt(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId) {

        return ResponseEntity.ok(paymentService.getReceipt(email, orderId));
    }

    // Request refund
    // POST /api/customer/payments/1/refund
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<PaymentResponse> requestRefund(
            @AuthenticationPrincipal String email,
            @PathVariable Long orderId,
            @RequestBody RefundRequest request) throws StripeException {

        return ResponseEntity.ok(
                paymentService.requestRefund(email, orderId, request));
    }
}