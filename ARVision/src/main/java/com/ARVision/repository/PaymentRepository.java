package com.ARVision.repository;

import com.ARVision.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderOrderId(Long orderId);

    Optional<Payment> findByStripePaymentIntentId(String paymentIntentId);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByStatusOrderByPaymentDateDesc(
            Payment.PaymentStatus status, Pageable pageable);

    @Query("""
        SELECT p FROM Payment p
        WHERE (:status IS NULL OR p.status = :status)
        ORDER BY p.paymentDate DESC
    """)
    Page<Payment> findAllWithFilter(
            @Param("status") Payment.PaymentStatus status,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'COMPLETED'")
    Float getTotalRevenue();

    long countByStatus(Payment.PaymentStatus status);
}