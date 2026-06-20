package com.ARVision.service;

import com.ARVision.dto.armodel.ARModelResponse;
import com.ARVision.entity.ARModel;
import com.ARVision.entity.Product;
import com.ARVision.repository.ARModelRepository;
import com.ARVision.repository.ProductRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ARModelService {

    private final ARModelRepository arModelRepository;
    private final ProductRepository productRepository;
    private final Cloudinary cloudinary;

    // ── Map to response ────────────────────────────────────────
    private ARModelResponse toResponse(ARModel model) {
        return ARModelResponse.builder()
                .modelId(model.getModelId())
                .productId(model.getProduct().getProductId())
                .productName(model.getProduct().getName())
                .fileUrl(model.getFileUrl())
                .fileName(model.getFileName())
                .fileType(model.getFileType())
                .fileSize(model.getFileSize())
                .uploadedAt(model.getUploadedAt())
                .build();
    }

    // ── Upload AR model and link to product ────────────────────
    @Transactional
    public ARModelResponse uploadARModel(Long productId, MultipartFile file) throws IOException {

        // Validate product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate file type — only GLB and USDZ allowed
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("Invalid file");
        }

        String extension = originalFilename
                .substring(originalFilename.lastIndexOf(".") + 1)
                .toLowerCase();

        if (!extension.equals("glb") && !extension.equals("usdz")) {
            throw new RuntimeException("Only GLB and USDZ files are allowed for AR models");
        }

        // Validate file size — max 50MB
        float fileSizeMB = (float) file.getSize() / (1024 * 1024);
        if (fileSizeMB > 50) {
            throw new RuntimeException("File size exceeds 50MB limit");
        }

        // If product already has AR model → delete old one from cloudinary first
        if (arModelRepository.existsByProductProductId(productId)) {
            ARModel existing = arModelRepository.findByProductProductId(productId)
                    .orElseThrow();
            // Delete from cloudinary
            String publicId = existing.getFileName();
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "raw"
            ));
            arModelRepository.deleteByProductProductId(productId);
        }

        // Upload to Cloudinary as raw file
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "raw",
                        "folder",        "ar-models",
                        "public_id",     "product_" + productId + "_" + System.currentTimeMillis(),
                        "format",        extension
                )
        );

        String fileUrl    = (String) uploadResult.get("secure_url");
        String publicId   = (String) uploadResult.get("public_id");

        // Save AR model record
        ARModel arModel = new ARModel();
        arModel.setProduct(product);
        arModel.setFileUrl(fileUrl);
        arModel.setFileName(publicId);
        arModel.setFileType(extension.toUpperCase());
        arModel.setFileSize(fileSizeMB);

        ARModel saved = arModelRepository.save(arModel);
        return toResponse(saved);
    }

    // ── Get AR model by product ID ─────────────────────────────
    public ARModelResponse getARModelByProductId(Long productId) {
        ARModel model = arModelRepository.findByProductProductId(productId)
                .orElseThrow(() -> new RuntimeException("No AR model found for this product"));
        return toResponse(model);
    }

    // ── Get all AR models ──────────────────────────────────────
    public List<ARModelResponse> getAllARModels() {
        return arModelRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Delete AR model ────────────────────────────────────────
    @Transactional
    public void deleteARModel(Long productId) throws IOException {
        ARModel model = arModelRepository.findByProductProductId(productId)
                .orElseThrow(() -> new RuntimeException("No AR model found for this product"));

        // Delete from Cloudinary
        cloudinary.uploader().destroy(
                model.getFileName(),
                ObjectUtils.asMap("resource_type", "raw")
        );

        arModelRepository.delete(model);
    }
}