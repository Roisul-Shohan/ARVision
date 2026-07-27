package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.product.ProductResponse;
import com.ARVision.dto.review.RatingSummary;
import com.ARVision.dto.review.ReviewResponse;
import com.ARVision.service.ProductService;
import com.ARVision.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    // Home page — all products with pagination and sorting
    // GET /api/products?page=0&size=12&sortBy=createdAt&sortDir=desc
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(name = "page", defaultValue = "0") String page,
            @RequestParam(name = "size", defaultValue = "12") String size,
            @RequestParam(name = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        int pageNum = parseIntOrDefault(page, 0);
        int pageSize = Math.min(Math.max(parseIntOrDefault(size, 12), 1), 100);

        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProducts(pageNum, pageSize, sortBy, sortDir),
                "Products fetched successfully"));
    }

    // Single product detail page
    // GET /api/products/5
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductById(id),
                "Product fetched successfully"));
    }

    // Real-time search — frontend calls this on EVERY keystroke
    // GET /api/products/search?keyword=chair&page=0&size=12
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") String page,
            @RequestParam(name = "size", defaultValue = "12") String size) {

        int pageNum = parseIntOrDefault(page, 0);
        int pageSize = Math.min(Math.max(parseIntOrDefault(size, 12), 1), 100);

        return ResponseEntity.ok(ApiResponse.success(
                productService.search(keyword, pageNum, pageSize),
                "Search results fetched"));
    }

    // Advanced filter — keyword + category + price range
    // GET /api/products/filter?keyword=sofa&category=furniture&minPrice=100&maxPrice=500
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> filterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(name = "page", defaultValue = "0") String page,
            @RequestParam(name = "size", defaultValue = "12") String size,
            @RequestParam(name = "sortBy", defaultValue = "price") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir) {

        int pageNum = parseIntOrDefault(page, 0);
        int pageSize = Math.min(Math.max(parseIntOrDefault(size, 12), 1), 100);
        Float minP = parseFloatOrNull(minPrice);
        Float maxP = parseFloatOrNull(maxPrice);

        return ResponseEntity.ok(ApiResponse.success(
            productService.filterProducts(
                keyword, category, minP, maxP, pageNum, pageSize, sortBy, sortDir),
            "Products filtered successfully"));
    }

    private static Float parseFloatOrNull(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            String t = raw.trim();
            if (t.length() >= 2 && t.charAt(0) == '"' && t.charAt(t.length() - 1) == '"') {
                t = t.substring(1, t.length() - 1);
            }
            return Float.parseFloat(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // All categories for filter dropdown
    // GET /api/products/categories
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllCategories(),
                "Categories fetched successfully"));
    }

    // ── Reviews (public read) ───────────────────────────────────

    // GET /api/products/{id}/reviews?page=0&size=10&sortDir=desc
    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0") String page,
            // Accept as String and parse defensively — some clients send quoted
            // values like ?size="5" which Spring's int converter rejects with
            // "For input string: \"5\"" and 400s. Parsing manually falls back to the
            // default whenever the value can't be parsed as an int.
            @RequestParam(name = "size", defaultValue = "10") String size,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        int pageNum = parseIntOrDefault(page, 0);
        int pageSize = parseIntOrDefault(size, 10);
        if (pageSize <= 0 || pageSize > 100) pageSize = 10;     // sanity cap
        if (pageNum < 0) pageNum = 0;

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getReviewsByProduct(id, pageNum, pageSize, sortDir),
                "Reviews fetched successfully"));
    }

    private static int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            // Strip surrounding quotes if the client sent ?size="5"
            String trimmed = raw.trim();
            if (trimmed.length() >= 2
                    && trimmed.charAt(0) == '"'
                    && trimmed.charAt(trimmed.length() - 1) == '"') {
                trimmed = trimmed.substring(1, trimmed.length() - 1);
            }
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    // GET /api/products/{id}/rating
    @GetMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<RatingSummary>> getProductRating(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getRatingSummary(id),
                "Rating summary fetched successfully"));
    }
}