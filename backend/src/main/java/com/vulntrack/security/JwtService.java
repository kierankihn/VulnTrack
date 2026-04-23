package com.vulntrack.security;

import com.vulntrack.config.JwtProperties;
import com.vulntrack.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT secret is not configured. Set the JWT_SECRET environment variable to at least 32 bytes.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 bytes (256 bits) for HS256. Current length: " + keyBytes.length);
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .issuer(properties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.accessTokenTtl())))
            .claim("username", user.getUsername())
            .claim("role", user.getRole().name())
            .claim("devGroupId", user.getDevGroup() != null ? user.getDevGroup().getId() : null)
            .claim("tokenType", "access")
            .signWith(secretKey)
            .compact();
    }

    public String createRefreshToken(User user, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .issuer(properties.issuer())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(properties.refreshTokenTtl())))
            .id(jti)
            .claim("tokenType", "refresh")
            .signWith(secretKey)
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }
}
