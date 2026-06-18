package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false)
    private String name;

    private String description;
    private Float price;
    private String category;
    private Integer stockQuantity;
    private String imageUrl;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    private ARModel arModel;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}