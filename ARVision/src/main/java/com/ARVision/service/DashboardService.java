package com.ARVision.service;

import com.ARVision.dto.dashboard.*;
import com.ARVision.entity.Order;
import com.ARVision.entity.Payment;
import com.ARVision.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Overview ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview() {
        return DashboardOverviewResponse.builder()

                // Order stats
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository
                        .countByStatus(Order.OrderStatus.PENDING))
                .processingOrders(orderRepository
                        .countByStatus(Order.OrderStatus.PROCESSING))
                .shippedOrders(orderRepository
                        .countByStatus(Order.OrderStatus.SHIPPED))
                .deliveredOrders(orderRepository
                        .countByStatus(Order.OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository
                        .countByStatus(Order.OrderStatus.CANCELLED))

                // Revenue stats
                .totalRevenue(orderRepository.getTotalRevenue())
                .revenueThisMonth(orderRepository.getRevenueThisMonth())
                .revenueToday(orderRepository.getRevenueToday())

                // Payment stats
                .pendingPayments(paymentRepository
                        .countByStatus(Payment.PaymentStatus.PENDING))
                .completedPayments(paymentRepository
                        .countByStatus(Payment.PaymentStatus.COMPLETED))
                .failedPayments(paymentRepository
                        .countByStatus(Payment.PaymentStatus.FAILED))
                .refundedPayments(paymentRepository
                        .countByStatus(Payment.PaymentStatus.REFUNDED))
                .refundRequested(paymentRepository
                        .countByStatus(Payment.PaymentStatus.REFUND_REQUESTED))

                // Product stats
                .totalProducts(productRepository.count())
                .lowStockProducts(productRepository.countLowStock(10))
                .outOfStockProducts(productRepository.countByStockQuantity(0))
                .productsWithAR(productRepository.countProductsWithAR())

                // Customer stats
                .totalCustomers(customerRepository.count())
                .newCustomersThisMonth(customerRepository
                        .countNewCustomersThisMonth())
                .newCustomersToday(customerRepository
                        .countNewCustomersToday())

                .build();
    }

    // ── Sales Report ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(
            String fromDateStr, String toDateStr) {

        // Parse dates
        LocalDate fromDate = LocalDate.parse(fromDateStr, DATE_FORMAT);
        LocalDate toDate = LocalDate.parse(toDateStr, DATE_FORMAT);

        // Validate range
        if (fromDate.isAfter(toDate)) {
            throw new RuntimeException("From date cannot be after to date");
        }

        if (fromDate.plusDays(90).isBefore(toDate)) {
            throw new RuntimeException("Date range cannot exceed 90 days");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);

        // Get all orders in range
        List<Order> orders = orderRepository
                .findOrdersInDateRange(fromDateTime, toDateTime);

        long totalOrders = orders.size();
        long cancelledOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED)
                .count();

        float totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .reduce(0f, Float::sum);

        float avgOrderValue = totalOrders > 0
                ? totalRevenue / (totalOrders - cancelledOrders)
                : 0f;

        // Daily breakdown
        List<Object[]> dailyData = orderRepository
                .getDailyBreakdown(fromDateTime, toDateTime);

        List<SalesReportResponse.DailyStats> dailyStats = new ArrayList<>();
        for (Object[] row : dailyData) {
            dailyStats.add(SalesReportResponse.DailyStats.builder()
                    .date(row[0].toString())
                    .ordersCount(((Number) row[1]).longValue())
                    .revenue(((Number) row[2]).floatValue())
                    .build());
        }

        return SalesReportResponse.builder()
                .fromDate(fromDateStr)
                .toDate(toDateStr)
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .cancelledOrders(cancelledOrders)
                .dailyBreakdown(dailyStats)
                .build();
    }

    // ── Top Products ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(int limit) {
        List<Object[]> results = productRepository
                .findTopSellingProducts(PageRequest.of(0, limit));

        List<TopProductResponse> topProducts = new ArrayList<>();

        for (Object[] row : results) {
            topProducts.add(TopProductResponse.builder()
                    .productId(((Number) row[0]).longValue())
                    .productName((String) row[1])
                    .category((String) row[2])
                    .imageUrl((String) row[3])
                    .currentStock(((Number) row[4]).intValue())
                    .totalQuantitySold(((Number) row[5]).longValue())
                    .totalRevenue(((Number) row[6]).floatValue())
                    .hasArModel((Boolean) row[7])
                    .build());
        }

        return topProducts;
    }
}