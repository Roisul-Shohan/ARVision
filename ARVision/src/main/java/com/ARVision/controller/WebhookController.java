package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    // Stripe calls this automatically when payment succeeds/fai
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false)
            String sigHeader) {

        try {
            paymentService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok(ApiResponse.success(null, "Webhook received"));
        } catch (RuntimeException e) {
            System.out.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
        }
    }
}