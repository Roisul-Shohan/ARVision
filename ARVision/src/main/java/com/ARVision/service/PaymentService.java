package com.ARVision.service;

import com.ARVision.dto.payment.PaymentIntentResponse;
import com.ARVision.dto.payment.PaymentResponse;
import com.ARVision.dto.payment.RefundRequest;
import com.ARVision.entity.Customer;
import com.ARVision.entity.Order;
import com.ARVision.entity.Payment;
import com.ARVision.exception.BadRequestException;
import com.ARVision.exception.PaymentException;
import com.ARVision.exception.ResourceNotFoundException;
import com.ARVision.exception.UnauthorizedException;
import com.ARVision.repository.CustomerRepository;
import com.ARVision.repository.OrderRepository;
import com.ARVision.repository.PaymentRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

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

private Customer getCustomer(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", null));
    }

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

@Transactional
        public PaymentIntentResponse createPaymentIntent(String email, Long orderId)
                        throws StripeException {

            Customer customer = getCustomer(email);

            Order order = orderRepository.findByIdWithItems(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

            if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
                        throw new UnauthorizedException("Unauthorized to pay this order");
            }

            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                        throw new BadRequestException("Cannot pay for a cancelled order");
            }

            paymentRepository.findByOrderOrderId(orderId).ifPresent(p -> {
                        if (p.getStatus() == Payment.PaymentStatus.COMPLETED) {
                                throw new BadRequestException("Order is already paid");
                        }
            });

                long amountInCents = (long) (order.getTotalAmount() * 100);

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

                Payment payment = paymentRepository.findByOrderOrderId(orderId)
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

        @Transactional
        public void handleWebhook(String payload, String sigHeader) {
                Gson gson = new Gson();

                try {
                        if (verifyWebhook && sigHeader != null && !sigHeader.isBlank()) {
                                Event event = com.stripe.net.Webhook.constructEvent(
                                                payload, sigHeader, webhookSecret);
                                handleVerifiedWebhook(event);
                        } else {
                                JsonObject root = gson.fromJson(payload, JsonObject.class);
                                String type = root.get("type").getAsString();
                                JsonObject object = root.getAsJsonObject("data").getAsJsonObject("object");
                                handleSimulatedWebhook(type, object, gson);
                        }
                } catch (SignatureVerificationException e) {
                        throw new RuntimeException("Invalid Stripe webhook signature: " + e.getMessage());
                } catch (Exception e) {
                        throw new RuntimeException("Webhook parsing failed: " + e.getMessage(), e);
                }
        }

        private void handleVerifiedWebhook(Event event) {
                System.out.println("=== WEBHOOK EVENT: " + event.getType() + " ===");

                StripeObject stripeObject = event.getDataObjectDeserializer()
                                .getObject()
                                .orElseThrow(() -> new RuntimeException("Failed to deserialize webhook event data"));

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
                                Charge charge = (Charge) stripeObject;
                                System.out.println("Charge refunded: " + charge.getId());
                        }
                        default -> System.out.println("Unhandled event type: " + event.getType());
                }
        }

        private void handleSimulatedWebhook(String type, JsonObject object, Gson gson) {
                System.out.println("=== SIMULATED WEBHOOK EVENT: " + type + " ===");

                switch (type) {
                        case "payment_intent.succeeded" -> {
                                String paymentIntentId = object.get("id").getAsString();
                                String latestChargeId = object.has("latest_charge") && !object.get("latest_charge").isJsonNull()
                                        ? object.get("latest_charge").getAsString()
                                        : null;

                                System.out.println("Payment succeeded: " + paymentIntentId);
                                handlePaymentSuccess(paymentIntentId, latestChargeId);
                        }
                        case "payment_intent.payment_failed" -> {
                                String paymentIntentId = object.get("id").getAsString();
                                System.out.println("Payment failed: " + paymentIntentId);
                                handlePaymentFailure(paymentIntentId);
                        }
                        case "charge.refunded" -> {
                                Charge charge = gson.fromJson(object, Charge.class);
                                System.out.println("Charge refunded: " + charge.getId());
                        }
                        default -> System.out.println("Unhandled simulated event type: " + type);
                }
        }

        private void handlePaymentSuccess(PaymentIntent paymentIntent) {
                handlePaymentSuccess(paymentIntent.getId(), paymentIntent.getLatestCharge());
        }

        private void handlePaymentSuccess(String paymentIntentId, String latestChargeId) {
                paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                                .ifPresent(payment -> {
                                        payment.setStatus(Payment.PaymentStatus.COMPLETED);
                                        payment.setTransactionId(latestChargeId);

                                        Order order = payment.getOrder();
                                        order.setStatus(Order.OrderStatus.PROCESSING);
                                        orderRepository.save(order);

                                        paymentRepository.save(payment);
                                        System.out.println("Payment completed for order: " + order.getOrderNumber());
                                });
        }

        private void handlePaymentFailure(PaymentIntent paymentIntent) {
                handlePaymentFailure(paymentIntent.getId());
        }

        private void handlePaymentFailure(String paymentIntentId) {
                paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                                .ifPresent(payment -> {
                                        payment.setStatus(Payment.PaymentStatus.FAILED);
                                        paymentRepository.save(payment);
                                        System.out.println("Payment failed for intent: " + paymentIntentId);
                                });
        }

@Transactional
        public PaymentResponse getReceipt(String email, Long orderId) {
                Customer customer = getCustomer(email);

                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

                if (!order.getCustomer().getUserId().equals(customer.getUserId())) {
                        throw new UnauthorizedException("Unauthorized to view this receipt");
                }

                Payment payment = paymentRepository.findByOrderOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId));

                return toResponse(payment);
        }

@Transactional
        public PaymentResponse requestRefund(String email, Long orderId, RefundRequest request)
                        throws StripeException {

                Customer customer = getCustomer(email);

                Payment payment = paymentRepository.findByOrderOrderId(orderId)
                        .orElseThrow(() -> new ResourceNotFoundException("Payment", orderId));

                if (!payment.getOrder().getCustomer().getUserId().equals(customer.getUserId())) {
                        throw new UnauthorizedException("Unauthorized to refund this order");
                }

                if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                        throw new BadRequestException("Only completed payments can be refunded");
                }

                Order.OrderStatus orderStatus = payment.getOrder().getStatus();
                if (orderStatus == Order.OrderStatus.SHIPPED || orderStatus == Order.OrderStatus.DELIVERED) {
                        throw new BadRequestException("Cannot refund order that has been " + orderStatus.name().toLowerCase());
                }

                String chargeId = payment.getTransactionId();
                if (chargeId == null || chargeId.isBlank()) {
                        PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(
                                payment.getStripePaymentIntentId());
                        chargeId = stripePaymentIntent.getLatestCharge();

                        if (chargeId == null || chargeId.isBlank()) {
                                throw new PaymentException("This payment has not been successfully charged yet, so it cannot be refunded");
                        }

                        payment.setTransactionId(chargeId);
                        paymentRepository.save(payment);
                }

                try {
                        RefundCreateParams params = RefundCreateParams.builder()
                                .setCharge(chargeId)
                                .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                                .build();

                        Refund refund = Refund.create(params);

                        payment.setStatus(Payment.PaymentStatus.REFUNDED);
                        payment.setRefundReason(request.getReason());
                        payment.setStripeRefundId(refund.getId());
                        paymentRepository.save(payment);

                        Order order = payment.getOrder();
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);
                } catch (InvalidRequestException e) {
                        if (isMissingChargeRefundError(e)) {
                                System.out.println("Stripe charge not found for refund; applying local fallback refund for testing.");
                                simulateRefund(payment, request.getReason());
                        } else {
                                throw new PaymentException("Refund processing failed: " + e.getMessage());
                        }
                }

                return toResponse(payment);
        }

        private boolean isMissingChargeRefundError(InvalidRequestException e) {
                String message = e.getMessage();
                return message != null && (
                                message.contains("No such charge")
                                || message.contains("does not have a successful charge to refund")
                                || message.contains("resource_missing"));
        }

        private void simulateRefund(Payment payment, String reason) {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                payment.setRefundReason(reason);
                payment.setStripeRefundId("sim_refund_" + UUID.randomUUID());
                paymentRepository.save(payment);

                Order order = payment.getOrder();
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
        }

        public Page<PaymentResponse> getAllPayments(Payment.PaymentStatus status, int page, int size) {
                Pageable pageable = PageRequest.of(page, size);
                return paymentRepository.findAllWithFilter(status, pageable)
                                .map(this::toResponse);
        }

        public Map<String, Object> getPaymentStats() {
                return Map.of(
                                "totalRevenue", paymentRepository.getTotalRevenue(),
                                "pendingPayments", paymentRepository.countByStatus(Payment.PaymentStatus.PENDING),
                                "completedPayments", paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED),
                                "failedPayments", paymentRepository.countByStatus(Payment.PaymentStatus.FAILED),
                                "refundedPayments", paymentRepository.countByStatus(Payment.PaymentStatus.REFUNDED),
                                "refundRequested", paymentRepository.countByStatus(Payment.PaymentStatus.REFUND_REQUESTED)
                );
        }
}