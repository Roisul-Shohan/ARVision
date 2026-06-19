package com.ARVision.service;

import com.ARVision.entity.RefreshToken;
import com.ARVision.entity.User;
import com.ARVision.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private final RefreshTokenRepository refreshTokenRepository;

    // Step 1 — delete in its OWN transaction first, commit immediately
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExistingToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();  // force immediate DB commit
    }

    // Step 2 — then create new token
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        deleteExistingToken(user);  // committed before insert happens

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiry));
        token.setRevoked(false);

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }

        return refreshToken;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();
    }
}