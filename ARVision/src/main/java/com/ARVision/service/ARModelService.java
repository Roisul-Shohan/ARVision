package com.ARVision.service;

import com.ARVision.dto.armodel.ARModelResponse;
import com.ARVision.entity.ARModel;
import com.ARVision.entity.Product;
import com.ARVision.exception.BadRequestException;
import com.ARVision.exception.ResourceNotFoundException;
import com.ARVision.repository.ARModelRepository;
import com.ARVision.repository.ProductRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ARModelService {

        private final ARModelRepository arModelRepository;
        private final ProductRepository productRepository;
        private final Cloudinary cloudinary;

        // RestTemplate has no SDK-imposed file size limits — used to bypass
        // cloudinary-http45's 10 MB check
        private final RestTemplate restTemplate = new RestTemplate();

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
                                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

                // Validate file type — only GLB and USDZ allowed
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                        throw new BadRequestException("Invalid file");
                }

                String extension = originalFilename
                                .substring(originalFilename.lastIndexOf(".") + 1)
                                .toLowerCase();

                if (!extension.equals("glb") && !extension.equals("usdz")) {
                        throw new BadRequestException("Only GLB and USDZ files are allowed for AR models");
                }

                // Validate file size — max 100MB (must match
                // spring.servlet.multipart.max-file-size)
                float fileSizeMB = (float) file.getSize() / (1024 * 1024);
                if (fileSizeMB > 100) {
                        throw new BadRequestException("File size exceeds 100MB limit (got " + fileSizeMB + "MB)");
                }

                // If product already has AR model → delete old one from Cloudinary first
                if (arModelRepository.existsByProductProductId(productId)) {
                        ARModel existing = arModelRepository.findByProductProductId(productId).orElseThrow();
                        cloudinary.uploader().destroy(existing.getFileName(),
                                        ObjectUtils.asMap("resource_type", "raw"));
                        arModelRepository.deleteByProductProductId(productId);
                }

                // Write multipart upload to temp file so we can stream it without buffering in
                // memory
                File tempFile = Files.createTempFile("ar-upload-", "." + extension).toFile();
                try {
                        file.transferTo(tempFile);

                        // ── Direct REST upload to Cloudinary (bypasses SDK transport's 10 MB limit) ──
                        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                        String publicId = "product_" + productId + "_" + timestamp;

                        // Params to sign (alphabetical, exclude api_key / file / resource_type /
                        // cloud_name)
                        TreeMap<String, String> signParams = new TreeMap<>();
                        signParams.put("folder", "ar-models");
                        signParams.put("format", extension);
                        signParams.put("public_id", publicId);
                        signParams.put("timestamp", timestamp);

                        String signature = cloudinarySign(signParams);

                        // Build multipart body
                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", new FileSystemResource(tempFile));
                        body.add("api_key", cloudinary.config.apiKey);
                        body.add("timestamp", timestamp);
                        body.add("signature", signature);
                        body.add("public_id", publicId);
                        body.add("folder", "ar-models");
                        body.add("format", extension);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        String uploadUrl = "https://api.cloudinary.com/v1_1/"
                                        + cloudinary.config.cloudName + "/raw/upload";

                        @SuppressWarnings("rawtypes")
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        uploadUrl,
                                        new HttpEntity<>(body, headers),
                                        Map.class);

                        Map<?, ?> uploadResult = response.getBody();
                        if (uploadResult == null || uploadResult.containsKey("error")) {
                                String errMsg = uploadResult != null
                                                ? uploadResult.get("error").toString()
                                                : "null response from Cloudinary";
                                throw new RuntimeException("Cloudinary upload failed: " + errMsg);
                        }

                        String fileUrl = (String) uploadResult.get("secure_url");
                        String uploadedPubId = (String) uploadResult.get("public_id");

                        // Save AR model record
                        ARModel arModel = new ARModel();
                        arModel.setProduct(product);
                        arModel.setFileUrl(fileUrl);
                        arModel.setFileName(uploadedPubId);
                        arModel.setFileType(extension.toUpperCase());
                        arModel.setFileSize(fileSizeMB);

                        return toResponse(arModelRepository.save(arModel));

                } finally {
                        tempFile.delete();
                }
        }

        // ── Get AR model by product ID ─────────────────────────────
        public ARModelResponse getARModelByProductId(Long productId) {
                ARModel model = arModelRepository.findByProductProductId(productId)
                                .orElseThrow(() -> new ResourceNotFoundException("No AR model found for this product"));
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
                                .orElseThrow(() -> new ResourceNotFoundException("No AR model found for this product"));

                cloudinary.uploader().destroy(model.getFileName(), ObjectUtils.asMap("resource_type", "raw"));
                arModelRepository.delete(model);
        }

        // ── Cloudinary SHA-1 signature helper ──────────────────────
        // Params must NOT include: api_key, file, resource_type, cloud_name, signature
        private String cloudinarySign(TreeMap<String, String> params) {
                try {
                        String toSign = String.join("&",
                                        params.entrySet().stream()
                                                        .map(e -> e.getKey() + "=" + e.getValue())
                                                        .toArray(String[]::new))
                                        + cloudinary.config.apiSecret;

                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        byte[] digest = md.digest(toSign.getBytes(StandardCharsets.UTF_8));

                        StringBuilder sb = new StringBuilder(40);
                        for (byte b : digest) {
                                sb.append(String.format("%02x", b));
                        }
                        return sb.toString();
                } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("SHA-1 unavailable", e);
                }
        }
}