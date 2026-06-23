package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.product.ProductRequest;
import com.ARVision.dto.product.ProductResponse;
import com.ARVision.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("authentication.credentials == 'SUPER_ADMIN' " +
        "or authentication.credentials == 'PRODUCT_MANAGER'")
public class AdminProductController {

    private final ProductService productService;

    // GET /api/admin/products?page=0&size=20
    @GetMapping
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProducts(page, size, "createdAt", "desc"),
                "Products fetched successfully"));
    }

    // POST /api/admin/products
    @PostMapping
        public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                productService.createProduct(request),
                "Product created successfully"));
    }

    // PUT /api/admin/products/5
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.updateProduct(id, request),
                "Product updated successfully"));
    }

    // DELETE /api/admin/products/5
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null,
                "Product deleted successfully"));
    }

    // PATCH /api/admin/products/5/stock?quantity=50
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.updateStock(id, quantity),
                "Stock updated successfully"));
    }

    // GET /api/admin/products/low-stock?threshold=10
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getLowStockProducts(threshold),
                "Low stock products fetched"));
    }
}