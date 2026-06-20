package com.ARVision.controller;

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
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(
                productService.getAllProducts(page, size, sortBy, sortDir));
    }

    // Single product detail page
    // GET /api/products/5
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // Real-time search — frontend calls this on EVERY keystroke
    // GET /api/products/search?keyword=chair&page=0&size=12
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        return ResponseEntity.ok(productService.search(keyword, page, size));
    }

    // Advanced filter — keyword + category + price range
    // GET /api/products/filter?keyword=sofa&category=furniture&minPrice=100&maxPrice=500
    @GetMapping("/filter")
    public ResponseEntity<Page<ProductResponse>> filterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Float minPrice,
            @RequestParam(required = false) Float maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        return ResponseEntity.ok(productService.filterProducts(
                keyword, category, minPrice, maxPrice, page, size, sortBy, sortDir));
    }

    // All categories for filter dropdown
    // GET /api/products/categories
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }
}