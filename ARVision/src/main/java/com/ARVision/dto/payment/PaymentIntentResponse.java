package com.ARVision.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentIntentResponse {
    private String clientSecret;      // frontend uses this to confirm payment
    private String paymentIntentId;   // stripe payment intent ID
    private Long amount;              // in cents/paisa
    private String currency;
    private String publishableKey;    // frontend needs this to init Stripe.js
    private Long orderId;
    private Float orderTotal;
}