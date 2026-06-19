package com.ARVision.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private String role;
    private String email;
    private String name;
    private Long userId;
    private String adminRole;      // SUPER_ADMIN, PRODUCT_MANAGER, ORDER_MANAGER, USER_MANAGER
    private String employeeId;
}