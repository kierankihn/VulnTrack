package com.vulntrack.service;

import com.vulntrack.config.JwtProperties;
import com.vulntrack.dto.AuthDto;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.RefreshTokenRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import com.vulntrack.security.JwtService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AuthServiceTransactionTest.Config.class)
class AuthServiceTransactionTest {

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        RefreshTokenRepository refreshTokenRepository() {
            return Mockito.mock(RefreshTokenRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return Mockito.mock(PasswordEncoder.class);
        }

        @Bean
        AuthenticatedUserProvider authenticatedUserProvider() {
            return Mockito.mock(AuthenticatedUserProvider.class);
        }

        @Bean
        JwtService jwtService() {
            return new JwtService(new JwtProperties(
                "vulntrack-backend",
                "change-me-please-change-me-please-change-me",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
            ));
        }

        @Bean
        RefreshTokenService refreshTokenService(RefreshTokenRepository refreshTokenRepository) {
            return new RefreshTokenService(refreshTokenRepository);
        }

        @Bean
        AuthService authService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthenticatedUserProvider authenticatedUserProvider
        ) {
            return new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, authenticatedUserProvider);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {
                }

                @Override
                protected void doCommit(DefaultTransactionStatus status) {
                }

                @Override
                protected void doRollback(DefaultTransactionStatus status) {
                }
            };
        }
    }

    @jakarta.annotation.Resource
    private AuthService authService;

    @jakarta.annotation.Resource
    private UserRepository userRepository;

    @jakarta.annotation.Resource
    private PasswordEncoder passwordEncoder;

    @jakarta.annotation.Resource
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void loginStoresRefreshTokenInsideWritableTransaction() {
        User activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("admin");
        activeUser.setEmail("admin@example.com");
        activeUser.setPasswordHash("$2a$10$encoded");
        activeUser.setRole(UserRole.ADMIN);
        activeUser.setActive(true);

        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setUsernameOrEmail("admin");
        request.setPassword("admin");

        when(userRepository.findByUsernameOrEmail("admin")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("admin", activeUser.getPasswordHash())).thenReturn(true);
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
            return invocation.getArgument(0);
        });

        authService.login(request);
    }
}
