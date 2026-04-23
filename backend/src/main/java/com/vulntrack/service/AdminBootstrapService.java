package com.vulntrack.service;

import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AdminBootstrapService {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@vulntrack.local";
    private static final String DEFAULT_ADMIN_FULL_NAME = "System Administrator";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void ensureDefaultAdmin() {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }

        User admin = userRepository.findByUsername(DEFAULT_ADMIN_USERNAME)
            .orElseGet(User::new);

        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            admin.setEmail(DEFAULT_ADMIN_EMAIL);
        }
        if (admin.getFullName() == null || admin.getFullName().isBlank()) {
            admin.setFullName(DEFAULT_ADMIN_FULL_NAME);
        }
        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        userRepository.save(admin);
        log.warn("No ADMIN user found. Bootstrapped default admin account with username '{}'.", DEFAULT_ADMIN_USERNAME);
    }
}
