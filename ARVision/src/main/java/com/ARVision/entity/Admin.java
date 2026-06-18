package com.ARVision.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "user_id")
public class Admin extends User {

    private String employeeId;

    @Enumerated(EnumType.STRING)
    private AdminRole adminrole;

    public enum AdminRole {
        SUPER_ADMIN,
        PRODUCT_MANAGER,
        ORDER_MANAGER,
        USER_MANAGER
    }
}