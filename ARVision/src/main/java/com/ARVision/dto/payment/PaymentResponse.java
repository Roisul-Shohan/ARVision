package com.ARVision.dto.payment;

import com.ARVision.entity.Payment;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private String stripePaymentIntentId;
    private String method;
    private Float amount;
    private Payment.PaymentStatus status;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String receiptUrl;          // Stripe receipt URL
    private boolean refundable;         // can customer request refund
}