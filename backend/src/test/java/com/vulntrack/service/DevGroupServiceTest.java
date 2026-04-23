package com.vulntrack.service;

import com.vulntrack.dto.DevGroupDto;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.DevGroupRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevGroupServiceTest {

    @Test
    void updateRejectsDuplicateGroupNameOwnedByAnotherGroup() {
        DevGroupRepository repo = mock(DevGroupRepository.class);
        DevGroupService devGroupService = new DevGroupService(repo, new AuthorizationService(mock(AuthenticatedUserProvider.class)), mock(UserRepository.class));

        DevGroup existing = new DevGroup();
        existing.setId(1L);
        existing.setName("Blue Team");

        DevGroup other = new DevGroup();
        other.setId(2L);
        other.setName("Red Team");

        DevGroupDto.CreateRequest req = new DevGroupDto.CreateRequest();
        req.setName("Red Team");
        req.setDescription("Updated");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.findByName("Red Team")).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> devGroupService.update(1L, req));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("Dev group name already exists", ex.getReason());
        verify(repo, never()).save(any(DevGroup.class));
    }

    @Test
    void findAllReturnsOnlyCurrentUsersGroupForNonAdmin() {
        DevGroupRepository repo = mock(DevGroupRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        DevGroupService devGroupService = new DevGroupService(repo, new AuthorizationService(authenticatedUserProvider), mock(UserRepository.class));

        DevGroup group = new DevGroup();
        group.setId(1L);
        group.setName("Blue Team");

        User tester = new User();
        tester.setId(2L);
        tester.setRole(UserRole.TESTER);
        tester.setDevGroup(group);
        tester.setActive(true);

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(tester);

        List<DevGroupDto> groups = devGroupService.findAll();

        assertEquals(1, groups.size());
        assertEquals("Blue Team", groups.get(0).getName());
    }

    @Test
    void updateMovesLeaderOffPreviouslyLedGroup() {
        DevGroupRepository repo = mock(DevGroupRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        DevGroupService devGroupService = new DevGroupService(repo, new AuthorizationService(mock(AuthenticatedUserProvider.class)), userRepository);

        DevGroup source = new DevGroup();
        source.setId(1L);
        source.setName("Source");

        DevGroup target = new DevGroup();
        target.setId(2L);
        target.setName("Target");

        User leader = new User();
        leader.setId(9L);
        leader.setFullName("Alice");
        leader.setRole(UserRole.GROUP_LEAD);
        leader.setDevGroup(source);
        source.setLeader(leader);

        DevGroupDto.CreateRequest req = new DevGroupDto.CreateRequest();
        req.setName("Target");
        req.setLeaderId(9L);

        when(repo.findById(2L)).thenReturn(Optional.of(target));
        when(repo.findByName("Target")).thenReturn(Optional.of(target));
        when(repo.findByLeaderId(9L)).thenReturn(Optional.of(source));
        when(userRepository.findById(9L)).thenReturn(Optional.of(leader));
        when(repo.save(any(DevGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DevGroupDto dto = devGroupService.update(2L, req);

        assertEquals(9L, dto.getLeaderId());
        assertEquals(target, leader.getDevGroup());
        assertEquals(leader, target.getLeader());
        assertEquals(null, source.getLeader());
    }
}
