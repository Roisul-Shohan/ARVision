package com.ARVision.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;

    // ── Optional password change ────────────────────────────────
    // Both fields must be provided together to change password.
    // Left blank / null → password is NOT updated.
    // No @Size here on purpose — length validation happens in the service
    // only when the user actually types a new password.
    private String currentPassword;
    private String newPassword;
}
