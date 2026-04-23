package com.vulntrack.service;

import com.vulntrack.config.JwtProperties;
import com.vulntrack.dto.AuthDto;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.RefreshToken;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.RefreshTokenRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import com.vulntrack.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void loginReturnsAccessAndRefreshTokensForActiveUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenService refreshTokenService = new RefreshTokenService(mock(RefreshTokenRepository.class));
        JwtService jwtService = new JwtService(new JwtProperties(
            "vulntrack-backend",
            "change-me-please-change-me-please-change-me",
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        ));
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, mock(AuthenticatedUserProvider.class));

        DevGroup devGroup = new DevGroup();
        devGroup.setId(3L);
        devGroup.setName("Core Platform");

        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("alice");
        activeUser.setEmail("alice@example.com");
        activeUser.setFullName("Alice");
        activeUser.setPasswordHash("$2a$10$encoded");
        activeUser.setRole(UserRole.GROUP_LEAD);
        activeUser.setActive(true);
        activeUser.setDevGroup(devGroup);

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setUsernameOrEmail("alice");
        request.setPassword("secret123");

        when(userRepository.findByUsernameOrEmail("alice")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("secret123", activeUser.getPasswordHash())).thenReturn(true);

        AuthDto.TokenResponse response = authService.login(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(UserRole.GROUP_LEAD, response.getUser().getRole());
        assertEquals(3L, response.getUser().getDevGroupId());
    }

    @Test
    void loginRejectsInactiveUser() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenService refreshTokenService = new RefreshTokenService(mock(RefreshTokenRepository.class));
        JwtService jwtService = new JwtService(new JwtProperties(
            "vulntrack-backend",
            "change-me-please-change-me-please-change-me",
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        ));
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, mock(AuthenticatedUserProvider.class));

        User inactiveUser = new User();
        inactiveUser.setId(1L);
        inactiveUser.setUsername("alice");
        inactiveUser.setPasswordHash("$2a$10$encoded");
        inactiveUser.setActive(false);

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setUsernameOrEmail("alice");
        request.setPassword("secret123");

        when(userRepository.findByUsernameOrEmail("alice")).thenReturn(Optional.of(inactiveUser));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> authService.login(request));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void refreshReturnsNewTokensForActiveStoredSession() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        JwtService jwtService = new JwtService(new JwtProperties(
            "vulntrack-backend",
            "change-me-please-change-me-please-change-me",
            Duration.ofMinutes(15),
            Duration.ofDays(7)
        ));
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, mock(AuthenticatedUserProvider.class));

        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("alice");
        activeUser.setPasswordHash("$2a$10$encoded");
        activeUser.setRole(UserRole.ADMIN);
        activeUser.setActive(true);

        String refreshJwt = jwtService.createRefreshToken(activeUser, "refresh-jti");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setJti("refresh-jti");
        refreshToken.setUser(activeUser);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        AuthDto.RefreshRequest request = new AuthDto.RefreshRequest();
        request.setRefreshToken(refreshJwt);

        when(refreshTokenRepository.findByJti("refresh-jti")).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

        AuthDto.TokenResponse response = authService.refresh(request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals(UserRole.ADMIN, response.getUser().getRole());
    }
}
