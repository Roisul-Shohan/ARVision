package com.ARVision.controller;

import com.ARVision.dto.armodel.ARModelResponse;
import com.ARVision.service.ARModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ARModelController {

    private final ARModelService arModelService;

    // ── PUBLIC: Get AR model for a product (frontend AR viewer) ──
    // GET /api/products/5/ar-model
    @GetMapping("/products/{productId}/ar-model")
    public ResponseEntity<ARModelResponse> getARModel(@PathVariable Long productId) {

        return ResponseEntity.ok(arModelService.getARModelByProductId(productId));
    }

    // ── ADMIN: Upload AR model for a product ───────────────────
    // POST /api/admin/products/5/ar-model
    @PostMapping(
            value = "/admin/products/{productId}/ar-model",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ARModelResponse> uploadARModel(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file) throws IOException {
        System.out.println("jfj");
        return ResponseEntity.ok(arModelService.uploadARModel(productId, file));
    }

    // ── ADMIN: Get all AR models ───────────────────────────────
    // GET /api/admin/ar-models
    @GetMapping("/admin/ar-models")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<List<ARModelResponse>> getAllARModels() {
        return ResponseEntity.ok(arModelService.getAllARModels());
    }

    // ── ADMIN: Delete AR model ─────────────────────────────────
    // DELETE /api/admin/products/5/ar-model
    @DeleteMapping("/admin/products/{productId}/ar-model")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<String> deleteARModel(@PathVariable Long productId) throws IOException {
        arModelService.deleteARModel(productId);
        return ResponseEntity.ok("AR model deleted successfully");
    }
}