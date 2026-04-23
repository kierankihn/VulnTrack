package com.vulntrack.service;

import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.UserDto;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.DevGroupRepository;
import com.vulntrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserRepository repo;
    private final DevGroupService devGroupService;
    private final DevGroupRepository devGroupRepo;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> search(String q, UserRole role, int page, int size) {
        return PageResponse.of(
            repo.search(normalizeSearchPattern(q), role, PageRequest.of(page, size)).map(UserDto::from)
        );
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return UserDto.from(getOrThrow(id));
    }

    public UserDto create(UserDto.CreateRequest req) {
        if (repo.findByUsername(req.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (repo.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        validatePasswordStrength(req.getPassword());
        UserRole role = req.getRole() != null ? req.getRole() : UserRole.DEVELOPER;
        validateManualGroupLeadRole(null, role, req.getDevGroupId());
        validateDevGroupRequirement(role, req.getDevGroupId());
        User u = new User();
        u.setUsername(req.getUsername());
        u.setEmail(req.getEmail());
        u.setFullName(req.getFullName());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(role);
        if (req.getDevGroupId() != null) {
            u.setDevGroup(devGroupService.getOrThrow(req.getDevGroupId()));
        }
        User saved = repo.save(u);
        syncLeadership(saved);
        return UserDto.from(saved);
    }

    public UserDto update(Long id, UserDto.CreateRequest req) {
        User u = getOrThrow(id);
        repo.findByEmail(req.getEmail())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            });
        UserRole role = req.getRole() != null ? req.getRole() : u.getRole();
        validateManualGroupLeadRole(u, role, req.getDevGroupId());
        validateDevGroupRequirement(role, req.getDevGroupId());
        u.setEmail(req.getEmail());
        u.setFullName(req.getFullName());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getDevGroupId() != null) {
            u.setDevGroup(devGroupService.getOrThrow(req.getDevGroupId()));
        } else {
            u.setDevGroup(null);
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            validatePasswordStrength(req.getPassword());
            u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }
        User saved = repo.save(u);
        syncLeadership(saved);
        return UserDto.from(saved);
    }

    public UserDto setActive(Long id, boolean active) {
        User u = getOrThrow(id);
        u.setActive(active);
        if (!active) {
            refreshTokenService.revokeAllForUser(id);
        }
        return UserDto.from(repo.save(u));
    }

    public void resetPassword(Long id, String newPassword) {
        validatePasswordStrength(newPassword);
        User u = getOrThrow(id);
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        repo.save(u);
        refreshTokenService.revokeAllForUser(id);
    }

    public void changeOwnPassword(User user, String currentPassword, String newPassword) {
        validatePasswordStrength(newPassword);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repo.save(user);
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Password must not exceed " + MAX_PASSWORD_LENGTH + " characters");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> listAssignable() {
        User actor = authorizationService.getCurrentUser();
        java.util.List<UserDto> list;
        if (actor.getRole() == UserRole.ADMIN) {
            list = repo.search(null, null, PageRequest.of(0, 500)).stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(UserDto::from).toList();
        } else if (actor.getDevGroup() != null) {
            Long gid = actor.getDevGroup().getId();
            list = repo.search(null, null, PageRequest.of(0, 500)).stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .filter(u -> u.getDevGroup() != null && gid.equals(u.getDevGroup().getId()))
                .map(UserDto::from).toList();
        } else {
            list = java.util.List.of();
        }
        org.springframework.data.domain.Page<UserDto> page = new org.springframework.data.domain.PageImpl<>(list);
        return PageResponse.of(page);
    }

    public User getOrThrow(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    private void syncLeadership(User user) {
        Optional<DevGroup> previouslyLeading = devGroupRepo.findByLeaderId(user.getId());

        boolean shouldLead = user.getRole() == UserRole.GROUP_LEAD && user.getDevGroup() != null;
        DevGroup targetGroup = shouldLead ? user.getDevGroup() : null;

        // If user was leading a different group (or shouldn't lead anymore), release.
        previouslyLeading.ifPresent(old -> {
            if (targetGroup == null || !old.getId().equals(targetGroup.getId())) {
                old.setLeader(null);
                devGroupRepo.save(old);
            }
        });

        if (targetGroup != null) {
            User currentLeader = targetGroup.getLeader();
            if (currentLeader != null && !currentLeader.getId().equals(user.getId())) {
                if (currentLeader.getRole() == UserRole.GROUP_LEAD) {
                    currentLeader.setRole(UserRole.DEVELOPER);
                    repo.save(currentLeader);
                }
            }
            targetGroup.setLeader(user);
            devGroupRepo.save(targetGroup);
        }
    }

    private String normalizeSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private void validateDevGroupRequirement(UserRole role, Long devGroupId) {
        if (role != UserRole.ADMIN && devGroupId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Development group is required for non-admin users");
        }
    }

    private void validateManualGroupLeadRole(User existingUser, UserRole requestedRole, Long requestedDevGroupId) {
        if (requestedRole != UserRole.GROUP_LEAD) {
            if (existingUser != null && existingUser.getRole() == UserRole.GROUP_LEAD) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Manage group leaders via development group management");
            }
            return;
        }

        if (existingUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Assign group leaders via development group management");
        }

        Long currentDevGroupId = existingUser.getDevGroup() != null ? existingUser.getDevGroup().getId() : null;
        if (existingUser.getRole() != UserRole.GROUP_LEAD || !Objects.equals(currentDevGroupId, requestedDevGroupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Manage group leaders via development group management");
        }
    }
}
