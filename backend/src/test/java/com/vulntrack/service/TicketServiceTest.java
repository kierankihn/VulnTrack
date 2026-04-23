package com.vulntrack.service;

import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.Asset;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.TicketPriority;
import com.vulntrack.entity.TicketSource;
import com.vulntrack.entity.TicketStatus;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.AssetRepository;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.DevGroupRepository;
import com.vulntrack.repository.TicketRepository;
import com.vulntrack.repository.UserRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    @Test
    void createAppliesDefaultsAndAssociatesExistingEntities() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        CveRepository cveRepo = mock(CveRepository.class);
        AssetRepository assetRepo = mock(AssetRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AuthorizationService authorizationService = new AuthorizationService(authenticatedUserProvider);
        AssetService assetService = new AssetService(assetRepo, devGroupService(), mock(TicketRepository.class), authorizationService);
        UserService userService = userService(userRepo);
        TicketService ticketService = new TicketService(ticketRepo, cveRepo, assetService, userService, userRepo, authorizationService);

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        Asset asset = new Asset();
        asset.setId(10L);
        asset.setName("gateway");

        User assignee = new User();
        assignee.setId(20L);
        assignee.setFullName("Alice");

        User reporter = new User();
        reporter.setId(30L);
        reporter.setFullName("Bob");

        CveEntry cve = new CveEntry();
        cve.setId(40L);
        cve.setCveId("CVE-2026-0001");

        TicketDto.CreateRequest req = new TicketDto.CreateRequest();
        req.setTitle("Fix gateway vuln");
        req.setDescription("details");
        req.setAssetId(10L);
        req.setAssigneeId(20L);
        req.setReporterId(30L);
        req.setCveIds(List.of("CVE-2026-0001", "CVE-2026-9999"));

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(admin);
        when(assetRepo.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepo.findById(20L)).thenReturn(Optional.of(assignee));
        when(userRepo.findById(30L)).thenReturn(Optional.of(reporter));
        when(cveRepo.findByCveId("CVE-2026-0001")).thenReturn(Optional.of(cve));
        when(cveRepo.findByCveId("CVE-2026-9999")).thenReturn(Optional.empty());
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(99L);
            return ticket;
        });

        TicketDto dto = ticketService.create(req);

        assertEquals(TicketPriority.MEDIUM, dto.getPriority());
        assertEquals(TicketSource.MANUAL, dto.getSource());
        assertEquals(TicketStatus.OPEN, dto.getStatus());
        assertEquals(10L, dto.getAssetId());
        assertEquals(20L, dto.getAssigneeId());
        assertEquals(30L, dto.getReporterId());
        assertEquals(1, dto.getCves().size());
        assertTrue(dto.getCves().stream().anyMatch(item -> "CVE-2026-0001".equals(item.getCveId())));
        assertTrue(dto.getStatusHistory().get(0).contains("CREATED"));
    }

    @Test
    void testerCreateRejectsAssetOutsideOwnGroup() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        CveRepository cveRepo = mock(CveRepository.class);
        AssetRepository assetRepo = mock(AssetRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AuthorizationService authorizationService = new AuthorizationService(authenticatedUserProvider);
        AssetService assetService = new AssetService(assetRepo, devGroupService(), mock(TicketRepository.class), authorizationService);
        UserService userService = userService(userRepo);
        TicketService ticketService = new TicketService(ticketRepo, cveRepo, assetService, userService, userRepo, authorizationService);

        DevGroup testerGroup = new DevGroup();
        testerGroup.setId(1L);

        DevGroup otherGroup = new DevGroup();
        otherGroup.setId(2L);

        User tester = new User();
        tester.setId(100L);
        tester.setRole(UserRole.TESTER);
        tester.setDevGroup(testerGroup);
        tester.setActive(true);

        Asset otherGroupAsset = new Asset();
        otherGroupAsset.setId(9L);
        otherGroupAsset.setName("other-service");
        otherGroupAsset.setDevGroup(otherGroup);

        TicketDto.CreateRequest req = new TicketDto.CreateRequest();
        req.setTitle("Broken auth");
        req.setAssetId(9L);

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(tester);
        when(assetRepo.findById(9L)).thenReturn(Optional.of(otherGroupAsset));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ticketService.create(req));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void searchReturnsOnlyTicketsVisibleToTester() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AuthorizationService authorizationService = new AuthorizationService(authenticatedUserProvider);
        TicketService ticketService = new TicketService(
            ticketRepo,
            mock(CveRepository.class),
            null,
            null,
            mock(UserRepository.class),
            authorizationService
        );

        User tester = new User();
        tester.setId(30L);
        tester.setRole(UserRole.TESTER);
        tester.setActive(true);

        User sameReporter = new User();
        sameReporter.setId(30L);

        User anotherReporter = new User();
        anotherReporter.setId(31L);

        Ticket visibleTicket = new Ticket();
        visibleTicket.setId(1L);
        visibleTicket.setTitle("Visible");
        visibleTicket.setReporter(sameReporter);

        Ticket hiddenTicket = new Ticket();
        hiddenTicket.setId(2L);
        hiddenTicket.setTitle("Hidden");
        hiddenTicket.setReporter(anotherReporter);

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(tester);
        when(ticketRepo.searchAll(null, null, null, null, null)).thenReturn(List.of(visibleTicket, hiddenTicket));

        var response = ticketService.search(null, null, null, null, null, 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getId());
    }

    @Test
    void findByIdFiltersDeveloperTransitionsAtReviewStage() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AuthorizationService authorizationService = new AuthorizationService(authenticatedUserProvider);
        TicketService ticketService = new TicketService(
            ticketRepo,
            mock(CveRepository.class),
            null,
            null,
            mock(UserRepository.class),
            authorizationService
        );

        User developer = new User();
        developer.setId(7L);
        developer.setRole(UserRole.DEVELOPER);
        developer.setActive(true);

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setTitle("Review ready");
        ticket.setStatus(TicketStatus.IN_REVIEW);
        ticket.setAssignee(developer);

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(developer);
        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));

        TicketDto dto = ticketService.findById(1L);

        assertEquals(EnumSet.of(TicketStatus.WONT_FIX), dto.getAllowedTransitions());
    }

    @Test
    void transitionRejectsInvalidStatusChange() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(admin);
        TicketService ticketService = new TicketService(
            ticketRepo,
            mock(CveRepository.class),
            null,
            null,
            mock(UserRepository.class),
            new AuthorizationService(authenticatedUserProvider)
        );

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.OPEN);

        TicketDto.TransitionRequest req = new TicketDto.TransitionRequest();
        req.setTargetStatus(TicketStatus.RESOLVED);

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> ticketService.transition(1L, req));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void transitionToResolvedSetsResolvedTimestampAndHistory() {
        TicketRepository ticketRepo = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(admin);
        TicketService ticketService = new TicketService(
            ticketRepo,
            mock(CveRepository.class),
            null,
            null,
            mock(UserRepository.class),
            new AuthorizationService(authenticatedUserProvider)
        );

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setStatus(TicketStatus.IN_REVIEW);

        TicketDto.TransitionRequest req = new TicketDto.TransitionRequest();
        req.setTargetStatus(TicketStatus.RESOLVED);
        req.setComment("patched");

        when(ticketRepo.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDto dto = ticketService.transition(1L, req);

        assertEquals(TicketStatus.RESOLVED, dto.getStatus());
        assertNotNull(dto.getResolvedAt());
        assertTrue(dto.getStatusHistory().get(dto.getStatusHistory().size() - 1).contains("STATUS_CHANGED"));
        assertTrue(dto.getStatusHistory().get(dto.getStatusHistory().size() - 1).contains("patched"));
    }

    private UserService userService(UserRepository userRepo) {
        return new UserService(
            userRepo,
            devGroupService(),
            mock(DevGroupRepository.class),
            mock(PasswordEncoder.class),
            new RefreshTokenService(mock(com.vulntrack.repository.RefreshTokenRepository.class)),
            new AuthorizationService(mock(AuthenticatedUserProvider.class))
        );
    }

    private DevGroupService devGroupService() {
        return new DevGroupService(
            mock(DevGroupRepository.class),
            new AuthorizationService(mock(AuthenticatedUserProvider.class)),
            mock(UserRepository.class)
        );
    }
}
