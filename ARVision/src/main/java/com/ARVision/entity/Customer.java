package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "customers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Customer extends User {

    private LocalDate memberSince;
    private String shippingAddress;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    private Cart cart;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL,
            fetch = FetchType.EAGER)
    private List<Order> orders;
}