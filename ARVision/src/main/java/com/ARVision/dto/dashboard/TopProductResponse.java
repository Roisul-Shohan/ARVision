package com.ARVision.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TopProductResponse {
    private Long productId;
    private String productName;
    private String category;
    private String imageUrl;
    private long totalQuantitySold;
    private float totalRevenue;
    private int currentStock;
    private boolean hasArModel;
}