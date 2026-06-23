package com.ARVision.controller;

import com.ARVision.dto.auth.*;
import com.ARVision.dto.common.ApiResponse;
import com.ARVision.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-token-expiry}")
    private int refreshTokenExpiry;  // in milliseconds

    @Value("${cookie.secure:true}")        // ← add here
    private boolean cookieSecure;

    // ── Customer Register ──────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>>register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResult result = authService.registerCustomer(request);
        setRefreshTokenCookie(response, result.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(),
                "Registration successful"));
    }

    // ── Login ──────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResult result = authService.login(request);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(),
                "Login successful"));
    }


    // ── Refresh Token ──────────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        // Read refresh token from cookie (not request body)
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
                return ResponseEntity.status(401)
                    .body(ApiResponse.error("Refresh token not found"));
        }

        AuthResult result = authService.refreshToken(refreshToken);
        setRefreshTokenCookie(response, result.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(),
            "Token refreshed successfully"));
    }

    // ── Super Admin: Create Admin ──────────────────────────────

    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AuthResponse>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request,
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.createAdmin(request, email),
                "Admin created successfully"));
    }
    // ── Logout ─────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String email,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            authService.logout(email);
        }

        // Clear the cookie
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success(null,
            "Logged out successfully"));
    }

    // ── Cookie Helpers ─────────────────────────────────────────
    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);          // JS cannot access this
        cookie.setSecure(cookieSecure);            // HTTPS only (set false for local dev)
        cookie.setPath("/api/auth");       // Only sent to auth endpoints
        cookie.setMaxAge(refreshTokenExpiry / 1000);  // convert ms to seconds
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);              // Delete immediately
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}