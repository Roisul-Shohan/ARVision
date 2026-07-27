package com.ARVision.dto.review;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class RatingSummary {
    private Long productId;
    private double averageRating;     // 0.0 if no reviews
    private long totalReviews;
    private Map<Integer, Long> distribution;   // rating (1..5) -> count; missing buckets = 0
}
