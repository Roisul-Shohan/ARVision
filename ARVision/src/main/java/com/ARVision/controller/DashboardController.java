package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.dashboard.*;
import com.ARVision.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // all admin roles can see dashboard
public class DashboardController {

    private final DashboardService dashboardService;

    // GET /api/admin/dashboard/overview
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getOverview(),
                "Dashboard overview fetched"));
    }

    // GET /api/admin/dashboard/sales-report?from=2026-01-01&to=2026-06-30
    @GetMapping("/sales-report")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getSalesReport(from, to),
                "Sales report generated"));
    }

    // GET /api/admin/dashboard/top-products?limit=10
    @GetMapping("/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTopProducts(limit),
                "Top products fetched"));
    }
}