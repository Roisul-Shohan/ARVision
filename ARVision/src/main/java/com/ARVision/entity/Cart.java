package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Data
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER   // ← eager load
    )
    private List<CartItem> cartItems = new ArrayList<>();  // ← initialize

    private Float totalAmount = 0f;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    public void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}