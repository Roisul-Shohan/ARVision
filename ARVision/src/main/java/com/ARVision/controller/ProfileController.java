package com.ARVision.controller;

import com.ARVision.dto.common.ApiResponse;
import com.ARVision.dto.profile.ProfileResponse;
import com.ARVision.dto.profile.UpdateProfileRequest;
import com.ARVision.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class ProfileController {

    private final ProfileService profileService;

    // GET /api/customer/profile — load existing values into form
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.getProfile(email),
                "Profile fetched successfully"));
    }

    // PUT /api/customer/profile — update any field, optionally change password
    // Frontend sends all current values; if password fields are blank, password is left alone
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                profileService.updateProfile(email, request),
                "Profile updated successfully"));
    }
}
