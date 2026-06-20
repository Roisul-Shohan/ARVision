package com.ARVision.controller;

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
    public ResponseEntity<org.springframework.data.domain.Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                productService.getAllProducts(page, size, "createdAt", "desc"));
    }

    // POST /api/admin/products
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    // PUT /api/admin/products/5
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // DELETE /api/admin/products/5
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }

    // PATCH /api/admin/products/5/stock?quantity=50
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(productService.updateStock(id, quantity));
    }

    // GET /api/admin/products/low-stock?threshold=10
    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponse>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }
}