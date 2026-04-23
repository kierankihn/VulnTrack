package com.vulntrack.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_jti", columnList = "jti", unique = true),
    @Index(name = "idx_refresh_token_user", columnList = "user_id")
})
@Data
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 64)
    private String replacedByJti;

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
