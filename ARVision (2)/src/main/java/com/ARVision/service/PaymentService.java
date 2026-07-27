package com.ARVision.service;

import com.ARVision.dto.payment.*;
import com.ARVision.entity.*;
import com.ARVision.repository.*;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    @Value("${stripe.publishable-key}")
    private String publishableKey;

    @Value("${stripe.currency}")
    private String currency;

    @Value("${stripe.webhook-verify:true}")
    private boolean verifyWebhook;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    // ── Get customer ───────────────────────────────────────────
    private Customer getCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    // ── Map payment to response ────────────────────────────────
    private PaymentResponse toResponse(Payment payment) {
        boolean refundable = payment.getStatus() == Payment.PaymentStatus.COMPLETED
                && (payment.getOrder().getStatus() == Order.OrderStatus.PENDING
                || payment.getOrder().getStatus() == Order.OrderStatus.PROCESSING);

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrder().getOrderId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .method(payment.getMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .paymentDate(payment.getPaymentDate())
                .receiptUrl(payment.getReceiptUrl())
                .refundable(refundable)
                .build();
    }

    // ── Create Stripe PaymentIntent ────────────────────────────
    @Transactional
    public PaymentIntentResponse createPaymentIntent(
            String email, Long orderId) throws StripeException {

        Customer customer = getCustomer(email);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Security — customer can only pay their own order
        if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new RuntimeException("Unauthorized to pay this order");
        }

        // Check order status
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cannot pay for a cancelled order");
        }

        // Check if already paid
        paymentRepository.findByOrderOrderId(orderId).ifPresent(p -> {
            if (p.getStatus() == Payment.PaymentStatus.COMPLETED) {
                throw new RuntimeException("Order is already paid");
            }
        });

        // Convert amount to cents (Stripe uses smallest currency unit)
        long amountInCents = (long) (order.getTotalAmount() * 100);

        // Create Stripe PaymentIntent
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(currency)
                .setDescription("VisionCart Order #" + order.getOrderNumber())
                .putMetadata("orderId", orderId.toString())
                .putMetadata("customerEmail", email)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        // Save pending payment record
        Payment payment = paymentRepository
                .findByOrderOrderId(orderId)
                .orElse(new Payment());

        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod("STRIPE");
        payment.setStripePaymentIntentId(paymentIntent.getId());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return PaymentIntentResponse.builder()
                .clientSecret(paymentIntent.getClientSecret())
                .paymentIntentId(paymentIntent.getId())
                .amount(amountInCents)
                .currency(currency)
                .publishableKey(publishableKey)
                .orderId(orderId)
                .orderTotal(order.getTotalAmount())
                .build();
    }

    // ── Handle Stripe Webhook ──────────────────────────────────
    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        com.stripe.model.Event event;

        try {
            if (verifyWebhook && sigHeader != null) {
                // Production — verify signature
                event = com.stripe.net.Webhook.constructEvent(
                        payload, sigHeader, webhookSecret);
            } else {
                // Local dev/Postman testing — skip verification
                event = com.stripe.model.ApiResource.GSON.fromJson(
                        payload, com.stripe.model.Event.class);
            }
        } catch (com.stripe.exception.SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe webhook signature: "
                    + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Webhook parsing failed: "
                    + e.getMessage());
        }

        System.out.println("=== WEBHOOK EVENT: " + event.getType() + " ===");

        // Deserialize the event data object
        com.stripe.model.StripeObject stripeObject = event
                .getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new RuntimeException(
                        "Failed to deserialize webhook event data"));

        switch (event.getType()) {

            case "payment_intent.succeeded" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                System.out.println("Payment succeeded: " + paymentIntent.getId());
                handlePaymentSuccess(paymentIntent);
            }

            case "payment_intent.payment_failed" -> {
                PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
                System.out.println("Payment failed: " + paymentIntent.getId());
                handlePaymentFailure(paymentIntent);
            }

            case "charge.refunded" -> {
                com.stripe.model.Charge charge =
                        (com.stripe.model.Charge) stripeObject;
                System.out.println("Charge refunded: " + charge.getId());
            }

            default -> System.out.println("Unhandled event type: "
                    + event.getType());
        }
    }

    private void handlePaymentSuccess(PaymentIntent paymentIntent) {
        paymentRepository
                .findByStripePaymentIntentId(paymentIntent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.COMPLETED);
                    payment.setTransactionId(paymentIntent.getLatestCharge());

                    // Update order status to PROCESSING
                    Order order = payment.getOrder();
                    order.setStatus(Order.OrderStatus.PROCESSING);
                    orderRepository.save(order);

                    paymentRepository.save(payment);
                    System.out.println("Payment completed for order: "
                            + order.getOrderNumber());
                });
    }

    private void handlePaymentFailure(PaymentIntent paymentIntent) {
        paymentRepository
                .findByStripePaymentIntentId(paymentIntent.getId())
                .ifPresent(payment -> {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    System.out.println("Payment failed for intent: "
                            + paymentIntent.getId());
                });
    }

    // ── Get payment receipt ────────────────────────────────────
    @Transactional
    public PaymentResponse getReceipt(String email, Long orderId) {
        Customer customer = getCustomer(email);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new RuntimeException("Unauthorized");
        }

        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found for this order"));

        return toResponse(payment);
    }

    // ── Request refund ─────────────────────────────────────────
    @Transactional
    public PaymentResponse requestRefund(
            String email, Long orderId, RefundRequest request)
            throws StripeException {

        Customer customer = getCustomer(email);

        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "No payment found for this order"));

        // Security check
        if (!payment.getOrder().getCustomer().getUserId()
                .equals(customer.getUserId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Can only refund COMPLETED payments
        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException(
                    "Only completed payments can be refunded");
        }

        // Cannot refund SHIPPED or DELIVERED orders
        Order.OrderStatus orderStatus = payment.getOrder().getStatus();
        if (orderStatus == Order.OrderStatus.SHIPPED
                || orderStatus == Order.OrderStatus.DELIVERED) {
            throw new RuntimeException(
                    "Cannot refund order that has been "
                            + orderStatus.name().toLowerCase());
        }

        // Call Stripe refund API
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId())
                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                .build();

        Refund refund = Refund.create(params);

        // Update payment
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundReason(request.getReason());
        payment.setStripeRefundId(refund.getId());
        paymentRepository.save(payment);

        // Cancel the order
        Order order = payment.getOrder();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponse(payment);
    }

    // ── ADMIN: Get all payments ────────────────────────────────
    public Page<PaymentResponse> getAllPayments(
            Payment.PaymentStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        return paymentRepository.findAllWithFilter(status, pageable)
                .map(this::toResponse);
    }

    // ── ADMIN: Payment stats ───────────────────────────────────
    public Map<String, Object> getPaymentStats() {
        return Map.of(
                "totalRevenue",         paymentRepository.getTotalRevenue(),
                "pendingPayments",      paymentRepository
                        .countByStatus(Payment.PaymentStatus.PENDING),
                "completedPayments",    paymentRepository
                        .countByStatus(Payment.PaymentStatus.COMPLETED),
                "failedPayments",       paymentRepository
                        .countByStatus(Payment.PaymentStatus.FAILED),
                "refundedPayments",     paymentRepository
                        .countByStatus(Payment.PaymentStatus.REFUNDED),
                "refundRequested",      paymentRepository
                        .countByStatus(Payment.PaymentStatus.REFUND_REQUESTED)
        );
    }
}