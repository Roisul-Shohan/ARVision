package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.review.ReviewRequest;
import com.ARVision.dto.review.ReviewResponse;
import com.ARVision.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Upsert — create or update the current customer's review for a product
    // POST /api/customer/products/{productId}/reviews
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> submitReview(
            @AuthenticationPrincipal String email,
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.submitOrUpdateReview(email, productId, request),
                "Review submitted successfully"));
    }

    // Get the current customer's review for a product (404 if none yet)
    // GET /api/customer/products/{productId}/reviews/my
    @GetMapping("/products/{productId}/reviews/my")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @AuthenticationPrincipal String email,
            @PathVariable Long productId) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getMyReviewForProduct(email, productId),
                "Your review fetched successfully"));
    }

    // The current customer deletes their own review
    // DELETE /api/customer/reviews/{reviewId}
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteMyReview(
            @AuthenticationPrincipal String email,
            @PathVariable Long reviewId) {

        reviewService.deleteMyReview(email, reviewId);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Review deleted successfully"));
    }
}
