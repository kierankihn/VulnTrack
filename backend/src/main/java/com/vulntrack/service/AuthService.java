package com.vulntrack.service;

import com.vulntrack.dto.AuthDto;
import com.vulntrack.entity.User;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import com.vulntrack.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    // Dummy BCrypt hash used for constant-time comparison when the user does not exist.
    // This prevents timing-side-channel user enumeration on the login endpoint.
    private static final String DUMMY_PASSWORD_HASH =
        "$2a$10$7EqJtq98hPqEX7fNZaFWoOa3Qu6DmaXgU8U5oG5p8q6mE3b8a4YUi";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public AuthDto.TokenResponse login(AuthDto.LoginRequest req) {
        User user = userRepository.findByUsernameOrEmail(req.getUsernameOrEmail()).orElse(null);
        String hash = (user != null && Boolean.TRUE.equals(user.getActive()))
            ? user.getPasswordHash()
            : DUMMY_PASSWORD_HASH;
        boolean matches = passwordEncoder.matches(req.getPassword(), hash);
        if (user == null || !Boolean.TRUE.equals(user.getActive()) || !matches) {
            throw invalidCredentials();
        }
        return issueTokens(user);
    }

    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest req) {
        Claims claims = parseRefreshClaims(req.getRefreshToken());
        String jti = claims.getId();
        refreshTokenService.findByJti(jti).orElseThrow(this::invalidCredentials);

        User user = userRepository.findById(Long.parseLong(claims.getSubject()))
            .orElseThrow(this::invalidCredentials);
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw invalidCredentials();
        }

        refreshTokenService.revoke(jti);
        return issueTokens(user);
    }

    public void logout(AuthDto.LogoutRequest req) {
        Claims claims = parseRefreshClaims(req.getRefreshToken());
        refreshTokenService.revoke(claims.getId());
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthenticatedUserResponse me() {
        return AuthDto.AuthenticatedUserResponse.from(authenticatedUserProvider.getCurrentUserOrThrow());
    }

    private AuthDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtService.createAccessToken(user);
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.createRefreshToken(user, jti);

        refreshTokenService.store(
            user,
            jti,
            LocalDateTime.ofInstant(Instant.now().plus(jwtService.refreshTokenTtl()), ZoneId.systemDefault())
        );

        AuthDto.TokenResponse response = new AuthDto.TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtService.accessTokenTtl().toSeconds());
        response.setRefreshExpiresIn(jwtService.refreshTokenTtl().toSeconds());
        response.setUser(AuthDto.AuthenticatedUserResponse.from(user));
        return response;
    }

    private Claims parseRefreshClaims(String token) {
        try {
            Claims claims = jwtService.parseClaims(token);
            if (!"refresh".equals(claims.get("tokenType", String.class))) {
                throw invalidCredentials();
            }
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw invalidCredentials();
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
