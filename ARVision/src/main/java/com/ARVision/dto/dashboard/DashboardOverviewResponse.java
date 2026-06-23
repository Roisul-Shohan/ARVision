package com.ARVision.dto.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewResponse {

    // ── Order stats ────────────────────────────────────────────
    private long totalOrders;
    private long pendingOrders;
    private long processingOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private long cancelledOrders;

    // ── Revenue stats ──────────────────────────────────────────
    private float totalRevenue;
    private float revenueThisMonth;
    private float revenueToday;

    // ── Payment stats ──────────────────────────────────────────
    private long pendingPayments;
    private long completedPayments;
    private long failedPayments;
    private long refundedPayments;
    private long refundRequested;

    // ── Product stats ──────────────────────────────────────────
    private long totalProducts;
    private long lowStockProducts;    // stock <= 10
    private long outOfStockProducts;  // stock = 0
    private long productsWithAR;      // has AR model

    // ── Customer stats ─────────────────────────────────────────
    private long totalCustomers;
    private long newCustomersThisMonth;
    private long newCustomersToday;
}