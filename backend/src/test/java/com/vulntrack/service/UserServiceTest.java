package com.vulntrack.service;

import com.vulntrack.dto.UserDto;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.RefreshTokenRepository;
import com.vulntrack.repository.DevGroupRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void createRequiresDevGroupForNonAdminRole() {
        UserRepository repo = mock(UserRepository.class);
        UserService userService = service(repo, mock(PasswordEncoder.class), mock(RefreshTokenRepository.class));

        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setUsername("tester1");
        req.setEmail("tester1@example.com");
        req.setFullName("Tester One");
        req.setPassword("secret123");
        req.setRole(UserRole.TESTER);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.create(req));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Development group is required for non-admin users", ex.getReason());
    }

    @Test
    void createRejectsManualGroupLeadAssignment() {
        UserRepository repo = mock(UserRepository.class);
        DevGroupRepository devGroupRepo = mock(DevGroupRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = new UserService(
            repo,
            devGroupService(devGroupRepo, repo),
            devGroupRepo,
            passwordEncoder,
            new RefreshTokenService(mock(RefreshTokenRepository.class)),
            authorizationService()
        );

        DevGroup devGroup = new DevGroup();
        devGroup.setId(7L);
        devGroup.setName("Blue Team");

        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setUsername("lead1");
        req.setEmail("lead1@example.com");
        req.setFullName("Lead One");
        req.setPassword("secret123");
        req.setRole(UserRole.GROUP_LEAD);
        req.setDevGroupId(7L);

        when(repo.findByUsername("lead1")).thenReturn(Optional.empty());
        when(repo.findByEmail("lead1@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(devGroupRepo.findById(7L)).thenReturn(Optional.of(devGroup));
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.create(req));

        assertEquals(400, ex.getStatusCode().value());
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void createStoresEncodedPassword() {
        UserRepository repo = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserService userService = service(repo, passwordEncoder, mock(RefreshTokenRepository.class));

        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setUsername("admin1");
        req.setEmail("admin1@example.com");
        req.setFullName("Admin One");
        req.setPassword("secret123");
        req.setRole(UserRole.ADMIN);

        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(req);

        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void updateRejectsDuplicateEmailOwnedByAnotherUser() {
        UserRepository repo = mock(UserRepository.class);
        UserService userService = service(repo, mock(PasswordEncoder.class), mock(RefreshTokenRepository.class));

        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setEmail("alice@example.com");
        existing.setFullName("Alice");

        User other = new User();
        other.setId(2L);
        other.setUsername("bob");
        other.setEmail("bob@example.com");
        other.setFullName("Bob");

        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setUsername("alice");
        req.setEmail("bob@example.com");
        req.setFullName("Alice Updated");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByEmail("bob@example.com")).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(1L, req));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Email already exists", ex.getReason());
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void updateRejectsChangingExistingGroupLeaderThroughUserEditor() {
        UserRepository repo = mock(UserRepository.class);
        DevGroupRepository devGroupRepo = mock(DevGroupRepository.class);
        UserService userService = new UserService(
            repo,
            devGroupService(devGroupRepo, repo),
            devGroupRepo,
            mock(PasswordEncoder.class),
            new RefreshTokenService(mock(RefreshTokenRepository.class)),
            authorizationService()
        );

        DevGroup devGroup = new DevGroup();
        devGroup.setId(5L);
        devGroup.setName("Core");

        User currentLeader = new User();
        currentLeader.setId(10L);
        currentLeader.setUsername("lead");
        currentLeader.setEmail("lead@example.com");
        currentLeader.setFullName("Lead User");
        currentLeader.setRole(UserRole.GROUP_LEAD);
        currentLeader.setDevGroup(devGroup);

        UserDto.CreateRequest req = new UserDto.CreateRequest();
        req.setUsername("lead");
        req.setEmail("lead@example.com");
        req.setFullName("Lead User");
        req.setRole(UserRole.DEVELOPER);
        req.setDevGroupId(5L);

        when(repo.findById(10L)).thenReturn(Optional.of(currentLeader));
        when(repo.findByEmail("lead@example.com")).thenReturn(Optional.of(currentLeader));
        when(devGroupRepo.findById(5L)).thenReturn(Optional.of(devGroup));
        when(devGroupRepo.findByLeaderId(10L)).thenReturn(Optional.of(devGroup));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.update(10L, req));

        assertEquals(400, ex.getStatusCode().value());
        verify(repo, never()).save(any(User.class));
    }

    @Test
    void changeOwnPasswordRevokesRefreshTokens() {
        UserRepository repo = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
        UserService userService = service(repo, passwordEncoder, refreshTokenRepository);

        User user = new User();
        user.setId(42L);
        user.setPasswordHash("encoded-old");

        when(passwordEncoder.matches("old-password", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new");
        when(refreshTokenRepository.findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(eq(42L), any(LocalDateTime.class)))
            .thenReturn(List.of());

        userService.changeOwnPassword(user, "old-password", "new-password");

        assertEquals("encoded-new", user.getPasswordHash());
        verify(repo).save(user);
        verify(refreshTokenRepository).findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(eq(42L), any(LocalDateTime.class));
    }

    private UserService service(
        UserRepository repo,
        PasswordEncoder passwordEncoder,
        RefreshTokenRepository refreshTokenRepository
    ) {
        DevGroupRepository devGroupRepository = mock(DevGroupRepository.class);
        return new UserService(
            repo,
            devGroupService(devGroupRepository, repo),
            devGroupRepository,
            passwordEncoder,
            new RefreshTokenService(refreshTokenRepository),
            authorizationService()
        );
    }

    private DevGroupService devGroupService(DevGroupRepository devGroupRepository, UserRepository userRepository) {
        return new DevGroupService(devGroupRepository, authorizationService(), userRepository);
    }

    private AuthorizationService authorizationService() {
        return new AuthorizationService(mock(AuthenticatedUserProvider.class));
    }
}
