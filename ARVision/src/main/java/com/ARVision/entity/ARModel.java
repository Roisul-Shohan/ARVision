package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ar_models")
public class ARModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long modelId;

    private String fileUrl;
    private String fileName;
    private String fileType;   // GLB or USDZ
    private Float fileSize;

    @OneToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
    }
}