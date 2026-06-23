package com.ARVision.repository;

import com.ARVision.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Real-time search — matches name, description, category
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // Filter by category
    Page<Product> findByCategory(String category, Pageable pageable);

    // Filter by price range
    Page<Product> findByPriceBetween(Float minPrice, Float maxPrice, Pageable pageable);

    // Search + category filter combined
    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR
               LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
        AND (:minPrice IS NULL OR p.price >= :minPrice)
        AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        AND p.stockQuantity > 0
    """)
    Page<Product> filterProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("minPrice") Float minPrice,
            @Param("maxPrice") Float maxPrice,
            Pageable pageable
    );

    // Get all distinct categories (for filter dropdown)
    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();

    // Low stock alert for admin
    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= :threshold")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    // Check stock availability
    boolean existsByProductIdAndStockQuantityGreaterThan(Long productId, int quantity);
    // Count low stock
    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity <= :threshold " +
            "AND p.stockQuantity > 0")
    long countLowStock(@Param("threshold") int threshold);

    // Count out of stock
    long countByStockQuantity(int stockQuantity);

    // Count products with AR model
    @Query("SELECT COUNT(p) FROM Product p WHERE p.arModel IS NOT NULL")
    long countProductsWithAR();

    // Top selling products
    @Query("""
    SELECT p.productId,
           p.name,
           p.category,
           p.imageUrl,
           p.stockQuantity,
           COALESCE(SUM(oi.quantity), 0) as totalSold,
           COALESCE(SUM(oi.subtotal), 0) as totalRevenue,
           CASE WHEN p.arModel IS NOT NULL THEN true ELSE false END as hasAR
    FROM Product p
    LEFT JOIN OrderItem oi ON oi.product = p
    LEFT JOIN oi.order o ON o.status != 'CANCELLED'
    GROUP BY p.productId, p.name, p.category,
             p.imageUrl, p.stockQuantity, p.arModel
    ORDER BY totalSold DESC
""")
    List<Object[]> findTopSellingProducts(Pageable pageable);
}