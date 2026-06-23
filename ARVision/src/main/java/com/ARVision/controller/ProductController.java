package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.product.ProductResponse;
import com.ARVision.service.ProductService;
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

    // Home page — all products with pagination and sorting
    // GET /api/products?page=0&size=12&sortBy=createdAt&sortDir=desc
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProducts(page, size, sortBy, sortDir),
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.search(keyword, page, size),
                "Search results fetched"));
    }

    // Advanced filter — keyword + category + price range
    // GET /api/products/filter?keyword=sofa&category=furniture&minPrice=100&maxPrice=500
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> filterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Float minPrice,
            @RequestParam(required = false) Float maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        return ResponseEntity.ok(ApiResponse.success(
            productService.filterProducts(
                keyword, category, minPrice, maxPrice, page, size, sortBy, sortDir),
            "Products filtered successfully"));
    }

    // All categories for filter dropdown
    // GET /api/products/categories
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllCategories(),
                "Categories fetched successfully"));
    }
}