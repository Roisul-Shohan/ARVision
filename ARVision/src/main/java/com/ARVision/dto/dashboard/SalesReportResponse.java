package com.ARVision.dto.dashboard;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SalesReportResponse {

    private String fromDate;
    private String toDate;

    // ── Summary ────────────────────────────────────────────────
    private long totalOrders;
    private float totalRevenue;
    private float averageOrderValue;
    private long cancelledOrders;

    // ── Daily breakdown ────────────────────────────────────────
    private List<DailyStats> dailyBreakdown;

    @Data
    @Builder
    public static class DailyStats {
        private String date;
        private long ordersCount;
        private float revenue;
    }
}