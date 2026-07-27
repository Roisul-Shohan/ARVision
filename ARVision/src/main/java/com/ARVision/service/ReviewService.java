package com.ARVision.service;

import com.ARVision.dto.review.RatingSummary;
import com.ARVision.dto.review.ReviewRequest;
import com.ARVision.dto.review.ReviewResponse;
import com.ARVision.entity.Customer;
import com.ARVision.entity.Product;
import com.ARVision.entity.Review;
import com.ARVision.entity.User;
import com.ARVision.exception.BadRequestException;
import com.ARVision.exception.ResourceNotFoundException;
import com.ARVision.exception.UnauthorizedException;
import com.ARVision.repository.CustomerRepository;
import com.ARVision.repository.ProductRepository;
import com.ARVision.repository.ReviewRepository;
import com.ARVision.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    // ── helpers ─────────────────────────────────────────────────

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));
    }

    private Customer getCustomer(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private ReviewResponse toResponse(Review review, Long currentCustomerUserId) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProduct().getProductId())
                .customerId(review.getCustomer().getUserId())
                .customerName(review.getCustomer().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .ownedByCurrentCustomer(
                        currentCustomerUserId != null
                                && currentCustomerUserId.equals(review.getCustomer().getUserId()))
                .build();
    }

    // ── write ops ───────────────────────────────────────────────

    /**
     * Create a new review or update the existing one for this (customer, product).
     * Allowed for any logged-in CUSTOMER.
     */
    @Transactional
    public ReviewResponse submitOrUpdateReview(
            String email, Long productId, ReviewRequest request) {

        Customer customer = getCustomer(email);
        Product product = getProductOrThrow(productId);

        Optional<Review> existing =
                reviewRepository.findByProductAndCustomer_UserId(product, customer.getUserId());

        Review review = existing.orElseGet(Review::new);
        review.setProduct(product);
        review.setCustomer(customer);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        return toResponse(saved, customer.getUserId());
    }

    /**
     * Delete a review. Only the review's owner can delete it.
     */
    @Transactional
    public void deleteMyReview(String email, Long reviewId) {
        Customer customer = getCustomer(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + reviewId));

        if (!review.getCustomer().getUserId().equals(customer.getUserId())) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }
        reviewRepository.delete(review);
    }

    // ── read ops ────────────────────────────────────────────────

    /** Public paginated reviews for a product. */
    public Page<ReviewResponse> getReviewsByProduct(
            Long productId, int page, int size, String sortDir) {

        Product product = getProductOrThrow(productId);

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return reviewRepository
                .findByProductOrderByCreatedAtDesc(product, pageable)
                .map(review -> toResponse(review, null));
    }

    /** Public aggregate rating for a product. */
    public RatingSummary getRatingSummary(Long productId) {
        Product product = getProductOrThrow(productId);

        Object[] aggregate = reviewRepository.getRatingSummary(product);
        double avg = aggregate[0] == null ? 0.0 : ((Number) aggregate[0]).doubleValue();
        long total = aggregate[1] == null ? 0L : ((Number) aggregate[1]).longValue();

        Map<Integer, Long> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) distribution.put(i, 0L);
        for (Object[] row : reviewRepository.getRatingDistribution(product)) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
        }

        return RatingSummary.builder()
                .productId(productId)
                .averageRating(Math.round(avg * 100.0) / 100.0)   // 2 d.p.
                .totalReviews(total)
                .distribution(distribution)
                .build();
    }

    /** Customer fetches their own review for a product (null if none). */
    public ReviewResponse getMyReviewForProduct(String email, Long productId) {
        Customer customer = getCustomer(email);
        Product product = getProductOrThrow(productId);

        return reviewRepository
                .findByProductAndCustomer_UserId(product, customer.getUserId())
                .map(review -> toResponse(review, customer.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have not reviewed this product yet"));
    }
}
