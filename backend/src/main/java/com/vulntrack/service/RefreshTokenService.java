package com.vulntrack.service;

import com.vulntrack.entity.RefreshToken;
import com.vulntrack.entity.User;
import com.vulntrack.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void store(User user, String jti, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setJti(jti);
        refreshToken.setExpiresAt(expiresAt);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByJti(String jti) {
        return refreshTokenRepository.findByJti(jti)
            .filter(token -> !token.isRevoked())
            .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    public void revoke(String jti) {
        refreshTokenRepository.findByJti(jti).ifPresent(token -> {
            token.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }

    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(userId, LocalDateTime.now())
            .forEach(token -> {
                token.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(token);
            });
    }
}
