package com.ARVision.service;

import com.ARVision.dto.auth.*;
import com.ARVision.entity.*;
import com.ARVision.repository.*;
import com.ARVision.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AdminRepository adminRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ── Customer Register ──────────────────────────────────────
    @Transactional
    public AuthResult registerCustomer(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPassword(passwordEncoder.encode(request.getPassword()));
        customer.setPhone(request.getPhone());
        customer.setRole(User.Role.CUSTOMER);
        customer.setMemberSince(LocalDate.now());

        customerRepository.save(customer);

        String accessToken = jwtUtil.generateAccessToken(customer.getEmail(), "CUSTOMER",null);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(customer);

        AuthResponse authResponse= AuthResponse.builder()
                .accessToken(accessToken)
                .role("CUSTOMER")
                .email(customer.getEmail())
                .name(customer.getName())
                .userId(customer.getUserId())
                .build();
        return new AuthResult(authResponse, refreshToken.getToken());
    }

    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // ── Password check depends on who is logging in ────────────
        if (user.getRole() == User.Role.ADMIN) {
            // Admin password is hardcoded — just compare plain text
            if (!"123456".equals(request.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
        } else {
            // Customer password is bcrypt hashed
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
        }

        // ── Build response based on role ───────────────────────────
        if (user.getRole() == User.Role.ADMIN) {

            Admin admin = adminRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Admin record not found"));

            // Generate token with adminRole claim inside
            String accessToken = jwtUtil.generateAccessToken(
                    user.getEmail(),
                    user.getRole().name(),
                    admin.getAdminrole().name()
            );
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(accessToken)
                    .role(user.getRole().name())              // "ADMIN"
                    .adminRole(admin.getAdminrole().name())   // "SUPER_ADMIN" etc
                    .employeeId(admin.getEmployeeId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .userId(user.getUserId())
                    .build();

            return new AuthResult(authResponse, refreshToken.getToken());
        }

        // ── CUSTOMER response ──────────────────────────────────────
        String accessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                null   // no adminRole for customers
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .role(user.getRole().name())   // "CUSTOMER"
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getUserId())
                .build();

        return new AuthResult(authResponse, refreshToken.getToken());
    }

    public AuthResult refreshToken(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();

        if (user.getRole() == User.Role.ADMIN) {
            Admin admin = adminRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Admin record not found"));

            String newAccessToken = jwtUtil.generateAccessToken(
                    user.getEmail(),
                    user.getRole().name(),
                    admin.getAdminrole().name()
            );

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .role(user.getRole().name())
                    .adminRole(admin.getAdminrole().name())
                    .employeeId(admin.getEmployeeId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .userId(user.getUserId())
                    .build();

            return new AuthResult(authResponse, refreshToken.getToken());
        }

        // CUSTOMER
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                null
        );

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .role(user.getRole().name())
                .email(user.getEmail())
                .name(user.getName())
                .userId(user.getUserId())
                .build();

        return new AuthResult(authResponse, refreshToken.getToken());
    }
    // ── Super Admin: Create Admin ──────────────────────────────
    @Transactional
    public AuthResponse createAdmin(CreateAdminRequest request, String requesterEmail) {
        // Verify requester is SUPER_ADMIN
        Admin requester = adminRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Requester not found"));

        if (requester.getAdminrole() != Admin.AdminRole.SUPER_ADMIN) {
            throw new RuntimeException("Only SUPER_ADMIN can create new admins");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        Admin admin = new Admin();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setPhone(request.getPhone());
        admin.setEmployeeId(request.getEmployeeId());
        admin.setAdminrole(request.getAdminRole());
        admin.setRole(User.Role.ADMIN);

        Admin savedAdmin = adminRepository.save(admin);

        return AuthResponse.builder()
                .role("ADMIN")
                .adminRole(savedAdmin.getAdminrole().name())
                .employeeId(savedAdmin.getEmployeeId())
                .email(savedAdmin.getEmail())
                .name(savedAdmin.getName())
                .userId(savedAdmin.getUserId())
                .build();
    }

    // ── Logout ─────────────────────────────────────────────────
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenService.revokeToken(user);
    }


}