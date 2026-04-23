package com.vulntrack.service;

import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapServiceTest {

    @Test
    void ensureDefaultAdminCreatesAdminWhenMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapService bootstrapService = new AdminBootstrapService(userRepository, passwordEncoder);

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrapService.ensureDefaultAdmin();

        verify(userRepository).save(any(User.class));
    }

    @Test
    void ensureDefaultAdminSkipsWhenAdminAlreadyExists() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapService bootstrapService = new AdminBootstrapService(userRepository, passwordEncoder);

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(true);

        bootstrapService.ensureDefaultAdmin();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void ensureDefaultAdminPromotesExistingAdminUserRecord() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AdminBootstrapService bootstrapService = new AdminBootstrapService(userRepository, passwordEncoder);

        User existing = new User();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setEmail("legacy@example.com");
        existing.setFullName("Legacy User");
        existing.setRole(UserRole.DEVELOPER);
        existing.setActive(false);

        when(userRepository.existsByRole(UserRole.ADMIN)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("admin")).thenReturn("encoded-admin");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrapService.ensureDefaultAdmin();

        assertEquals(UserRole.ADMIN, existing.getRole());
        assertTrue(existing.getActive());
        assertEquals("encoded-admin", existing.getPasswordHash());
        assertEquals("legacy@example.com", existing.getEmail());
    }
}
