package com.ARVision.service;

import com.ARVision.dto.profile.ProfileResponse;
import com.ARVision.dto.profile.UpdateProfileRequest;
import com.ARVision.entity.Customer;
import com.ARVision.exception.BadRequestException;
import com.ARVision.exception.ResourceNotFoundException;
import com.ARVision.exception.UnauthorizedException;
import com.ARVision.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Get my profile ──────────────────────────────────────────
    public ProfileResponse getProfile(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", null));
        return toResponse(customer);
    }

    // ── Update my profile + optional password change ───────────
    @Transactional
    public ProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", null));

        // Always update profile fields
        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setShippingAddress(request.getShippingAddress());

        // ── Optional password change ────────────────────────────
        // Rule: if user types a new password, currentPassword must be correct.
        //       if both are blank, skip password logic entirely.
        String current = request.getCurrentPassword();
        String next    = request.getNewPassword();

        boolean hasCurrent = current != null && !current.isBlank();
        boolean hasNew     = next    != null && !next.isBlank();

        if (hasCurrent || hasNew) {
            // If user is trying to change password, BOTH fields are required
            if (!hasCurrent || !hasNew) {
                throw new BadRequestException(
                        "Both currentPassword and newPassword are required to change password");
            }

            // Only enforce length when user actually fills in a new password
            if (next.length() < 6) {
                throw new BadRequestException(
                        "New password must be at least 6 characters");
            }

            if (!passwordEncoder.matches(current, customer.getPassword())) {
                throw new UnauthorizedException("Current password is incorrect");
            }

            if (current.equals(next)) {
                throw new BadRequestException(
                        "New password must be different from current password");
            }

            customer.setPassword(passwordEncoder.encode(next));
        }

        Customer saved = customerRepository.save(customer);
        return toResponse(saved);
    }

    // ── Map entity to response ─────────────────────────────────
    private ProfileResponse toResponse(Customer customer) {
        return ProfileResponse.builder()
                .userId(customer.getUserId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .role(customer.getRole().name())
                .memberSince(customer.getMemberSince())
                .shippingAddress(customer.getShippingAddress())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}