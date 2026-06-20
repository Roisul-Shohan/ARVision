package com.ARVision.dto.product;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProductResponse {
    private Long productId;
    private String name;
    private String description;
    private Float price;
    private String category;
    private Integer stockQuantity;
    private String imageUrl;
    private boolean hasArModel;      // frontend shows AR button only if true
    private String arModelUrl;       // direct URL to GLB/USDZ file
    private LocalDateTime createdAt;
}