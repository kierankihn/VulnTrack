package com.vulntrack.repository;

import com.vulntrack.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    List<RefreshToken> findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(Long userId, LocalDateTime now);
}
