package com.ARVision.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResult {
    private AuthResponse authResponse;
    private String refreshToken;  // goes to cookie, never in JSON
}
