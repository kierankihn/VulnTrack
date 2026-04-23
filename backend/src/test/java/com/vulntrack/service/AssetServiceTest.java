package com.vulntrack.service;

import com.vulntrack.dto.AssetDto;
import com.vulntrack.entity.Asset;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.AssetRepository;
import com.vulntrack.repository.TicketRepository;
import com.vulntrack.security.AuthenticatedUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetServiceTest {

    @Test
    void findByIdIncludesOpenTicketCount() {
        AssetRepository repo = mock(AssetRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AssetService assetService = new AssetService(repo, null, ticketRepository, new AuthorizationService(authenticatedUserProvider));

        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);

        Asset asset = new Asset();
        asset.setId(1L);
        asset.setName("frontend");

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(admin);
        when(repo.findById(1L)).thenReturn(Optional.of(asset));
        when(ticketRepository.findOpenByAssetId(1L)).thenReturn(List.of(new Ticket(), new Ticket()));

        AssetDto dto = assetService.findById(1L);

        assertEquals(2, dto.getOpenTicketCount());
        assertEquals("frontend", dto.getName());
    }

    @Test
    void updateClearsDevGroupWhenRequestDoesNotProvideOne() {
        AssetRepository repo = mock(AssetRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        AssetService assetService = new AssetService(repo, null, ticketRepository, new AuthorizationService(mock(AuthenticatedUserProvider.class)));

        Asset asset = new Asset();
        asset.setId(1L);
        asset.setName("frontend");
        DevGroup devGroup = new DevGroup();
        devGroup.setId(9L);
        devGroup.setName("Blue Team");
        asset.setDevGroup(devGroup);

        AssetDto.CreateRequest req = new AssetDto.CreateRequest();
        req.setName("frontend");
        req.setDescription("updated");
        req.setRepoUrl("https://example.com/repo");
        req.setProjectName("frontend");

        when(repo.findById(1L)).thenReturn(Optional.of(asset));
        when(repo.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssetDto dto = assetService.update(1L, req);

        assertNull(asset.getDevGroup());
        assertNull(dto.getDevGroupId());
    }

    @Test
    void findByIdRejectsAssetOutsideCurrentUsersGroup() {
        AssetRepository repo = mock(AssetRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        AuthenticatedUserProvider authenticatedUserProvider = mock(AuthenticatedUserProvider.class);
        AssetService assetService = new AssetService(repo, null, ticketRepository, new AuthorizationService(authenticatedUserProvider));

        DevGroup blue = new DevGroup();
        blue.setId(9L);

        DevGroup red = new DevGroup();
        red.setId(10L);

        User developer = new User();
        developer.setId(2L);
        developer.setRole(UserRole.DEVELOPER);
        developer.setDevGroup(blue);
        developer.setActive(true);

        Asset asset = new Asset();
        asset.setId(1L);
        asset.setName("frontend");
        asset.setDevGroup(red);

        when(authenticatedUserProvider.getCurrentUserOrThrow()).thenReturn(developer);
        when(repo.findById(1L)).thenReturn(Optional.of(asset));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> assetService.findById(1L));

        assertEquals(404, ex.getStatusCode().value());
    }
}
