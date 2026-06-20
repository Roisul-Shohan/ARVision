package com.ARVision.service;

import com.ARVision.dto.product.ProductRequest;
import com.ARVision.dto.product.ProductResponse;
import com.ARVision.entity.Product;
import com.ARVision.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // ── Map entity to response ─────────────────────────────────
    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .productId(p.getProductId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .hasArModel(p.getArModel() != null)
                .arModelUrl(p.getArModel() != null ? p.getArModel().getFileUrl() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ── PUBLIC: Get all products (home page) ───────────────────
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    // ── PUBLIC: Get single product ─────────────────────────────
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toResponse(product);
    }

    // ── PUBLIC: Real-time search (called on every keystroke) ───
    public Page<ProductResponse> search(String keyword, int page, int size) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts(page, size, "createdAt", "desc");
        }
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchByKeyword(keyword.trim(), pageable)
                .map(this::toResponse);
    }

    // ── PUBLIC: Filter products ────────────────────────────────
    public Page<ProductResponse> filterProducts(
            String keyword,
            String category,
            Float minPrice,
            Float maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.filterProducts(keyword, category, minPrice, maxPrice, pageable)
                .map(this::toResponse);
    }

    // ── PUBLIC: Get all categories ─────────────────────────────
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    // ── ADMIN: Create product ──────────────────────────────────
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());

        return toResponse(productRepository.save(product));
    }

    // ── ADMIN: Update product ──────────────────────────────────
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(request.getCategory());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());

        return toResponse(productRepository.save(product));
    }

    // ── ADMIN: Delete product ──────────────────────────────────
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(id);
    }

    // ── ADMIN: Update stock only ───────────────────────────────
    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (quantity < 0) {
            throw new RuntimeException("Stock cannot be negative");
        }

        product.setStockQuantity(quantity);
        return toResponse(productRepository.save(product));
    }

    // ── ADMIN: Low stock alert ─────────────────────────────────
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}