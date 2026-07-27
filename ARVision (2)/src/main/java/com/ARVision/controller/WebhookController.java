package com.ARVision.controller;

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
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {  // ← required now

        try {
            paymentService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("Webhook received");
        } catch (RuntimeException e) {
            System.out.println("Webhook error: " + e.getMessage());
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }
}