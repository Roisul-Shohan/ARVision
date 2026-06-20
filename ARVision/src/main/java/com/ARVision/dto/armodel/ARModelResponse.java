package com.ARVision.dto.armodel;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ARModelResponse {
    private Long modelId;
    private Long productId;
    private String productName;
    private String fileUrl;       // URL frontend uses to load AR
    private String fileName;
    private String fileType;      // GLB or USDZ
    private Float fileSize;       // in MB
    private LocalDateTime uploadedAt;
}