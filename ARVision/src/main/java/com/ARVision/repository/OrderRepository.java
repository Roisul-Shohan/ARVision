package com.ARVision.repository;

import com.ARVision.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Customer's own orders
    Page<Order> findByCustomerUserIdOrderByOrderDateDesc(
            Long customerId, Pageable pageable);

    // Find by order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // Admin — filter by status
    Page<Order> findByStatusOrderByOrderDateDesc(
            Order.OrderStatus status, Pageable pageable);

    // Admin — search by customer email or order number
    @Query("""
        SELECT o FROM Order o
        WHERE (:keyword IS NULL
               OR LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(o.customer.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR o.status = :status)
        ORDER BY o.orderDate DESC
    """)
    Page<Order> searchOrders(
            @Param("keyword") String keyword,
            @Param("status") Order.OrderStatus status,
            Pageable pageable
    );

    // Count by status — for admin dashboard
    long countByStatus(Order.OrderStatus status);

    // Revenue total
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
            "WHERE o.status != 'CANCELLED'")
    Float getTotalRevenue();

    // ── NEW: Fetch order with all items + products + AR models ──
    // Avoids NullPointerException and N+1 queries
    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product p
        LEFT JOIN FETCH p.arModel
        WHERE o.orderId = :orderId
    """)
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

    // ── NEW: Fetch customer orders with items loaded ────────────
    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.product p
        LEFT JOIN FETCH p.arModel
        WHERE o.customer.userId = :customerId
        ORDER BY o.orderDate DESC
    """)
    java.util.List<Order> findByCustomerIdWithItems(
            @Param("customerId") Long customerId);
}