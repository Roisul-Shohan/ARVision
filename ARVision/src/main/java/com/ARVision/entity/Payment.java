package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String method;                    // STRIPE, CASH_ON_DELIVERY

    private Float amount;

    private String stripePaymentIntentId;     // stripe's payment intent ID

    private String transactionId;             // stripe charge ID after success

    private String receiptUrl;                // stripe hosted receipt URL

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String contactNumber;

    private String refundReason;

    private String stripeRefundId;            // stripe refund ID if refunded

    private LocalDateTime paymentDate;

    @PrePersist
    public void prePersist() {
        this.paymentDate = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUND_REQUESTED,
        REFUNDED
    }
}