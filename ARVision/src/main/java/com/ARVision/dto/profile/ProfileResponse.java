package com.ARVision.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private LocalDate memberSince;
    private String shippingAddress;
    private LocalDateTime createdAt;
}
