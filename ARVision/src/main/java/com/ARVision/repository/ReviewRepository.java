package com.ARVision.repository;

import com.ARVision.entity.Product;
import com.ARVision.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Paginated reviews for a product, newest first
    Page<Review> findByProductOrderByCreatedAtDesc(Product product, Pageable pageable);

    // One review per (customer, product)
    Optional<Review> findByProductAndCustomer_UserId(Product product, Long customerUserId);

    boolean existsByProductAndCustomer_UserId(Product product, Long customerUserId);

    // Aggregate: average rating + total count (single query)
    @Query("""
        SELECT COALESCE(AVG(r.rating), 0), COUNT(r)
        FROM Review r
        WHERE r.product = :product
    """)
    Object[] getRatingSummary(@Param("product") Product product);

    // Rating distribution for a product: returns 5 rows of [rating, count]
    @Query("""
        SELECT r.rating, COUNT(r)
        FROM Review r
        WHERE r.product = :product
        GROUP BY r.rating
        ORDER BY r.rating DESC
    """)
    java.util.List<Object[]> getRatingDistribution(@Param("product") Product product);
}
