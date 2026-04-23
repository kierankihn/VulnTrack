package com.vulntrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.dto.ScanReportDto;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.Asset;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.DevGroup;
import com.vulntrack.entity.Ticket;
import com.vulntrack.entity.User;
import com.vulntrack.repository.AssetRepository;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.TicketRepository;
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
import static org.mockito.Mockito.when;

class ExternalScanServiceTest {

    @Test
    void processScanReportCreatesTicketForNewCveAndAutoAssignsLeastLoadedMember() {
        AssetRepository assetRepo = mock(AssetRepository.class);
        CveRepository cveRepo = mock(CveRepository.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        TicketService ticketService = new TicketService(
            ticketRepo,
            cveRepo,
            null,
            null,
            userRepo,
            new AuthorizationService(mock(AuthenticatedUserProvider.class))
        );
        ExternalScanService externalScanService = new ExternalScanService(
            assetRepo,
            cveRepo,
            ticketRepo,
            userRepo,
            ticketService,
            new ObjectMapper()
        );

        DevGroup devGroup = new DevGroup();
        devGroup.setId(5L);

        Asset asset = new Asset();
        asset.setId(7L);
        asset.setName("gateway");
        asset.setProjectName("gateway");
        asset.setDevGroup(devGroup);

        User assignee = new User();
        assignee.setId(8L);
        assignee.setFullName("Alice");

        CveEntry cve = new CveEntry();
        cve.setId(9L);
        cve.setCveId("CVE-2026-0001");
        cve.setCvssV3Score(9.8);

        ScanReportDto.Vulnerability vulnerability = new ScanReportDto.Vulnerability();
        vulnerability.setName("CVE-2026-0001");
        vulnerability.setSeverity("HIGH");
        vulnerability.setDescription("Remote code execution");

        ScanReportDto.Dependency dependency = new ScanReportDto.Dependency();
        dependency.setFileName("pom.xml");
        dependency.setFilePath("/workspace/pom.xml");
        dependency.setVulnerabilities(List.of(vulnerability));

        ScanReportDto report = new ScanReportDto();
        report.setProjectName("gateway");
        report.setRepoUrl("https://example.com/org/gateway");
        report.setDependencies(List.of(dependency));

        when(assetRepo.findByProjectName("gateway")).thenReturn(Optional.of(asset));
        when(ticketRepo.existsOpenTicketForCveAndAsset("CVE-2026-0001", 7L)).thenReturn(false);
        when(cveRepo.findByCveId("CVE-2026-0001")).thenReturn(Optional.of(cve));
        when(userRepo.findLeastLoadedMemberInGroup(5L)).thenReturn(List.of(assignee));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(99L);
            return ticket;
        });

        List<TicketDto> created = externalScanService.processScanReport(report);

        assertEquals(1, created.size());
        assertEquals("Alice", created.get(0).getAssigneeName());
        assertEquals("CRITICAL", created.get(0).getPriority().name());
        assertEquals("AUTO_SCAN", created.get(0).getSource().name());
    }

    @Test
    void processScanReportSkipsDuplicateOpenTickets() {
        AssetRepository assetRepo = mock(AssetRepository.class);
        CveRepository cveRepo = mock(CveRepository.class);
        TicketRepository ticketRepo = mock(TicketRepository.class);
        UserRepository userRepo = mock(UserRepository.class);
        ExternalScanService externalScanService = new ExternalScanService(
            assetRepo,
            cveRepo,
            ticketRepo,
            userRepo,
            new TicketService(
                ticketRepo,
                cveRepo,
                null,
                null,
                userRepo,
                new AuthorizationService(mock(AuthenticatedUserProvider.class))
            ),
            new ObjectMapper()
        );

        Asset asset = new Asset();
        asset.setId(7L);
        asset.setProjectName("gateway");

        ScanReportDto.Vulnerability vulnerability = new ScanReportDto.Vulnerability();
        vulnerability.setName("CVE-2026-0001");

        ScanReportDto.Dependency dependency = new ScanReportDto.Dependency();
        dependency.setVulnerabilities(List.of(vulnerability));

        ScanReportDto report = new ScanReportDto();
        report.setProjectName("gateway");
        report.setDependencies(List.of(dependency));

        when(assetRepo.findByProjectName("gateway")).thenReturn(Optional.of(asset));
        when(ticketRepo.existsOpenTicketForCveAndAsset("CVE-2026-0001", 7L)).thenReturn(true);

        List<TicketDto> created = externalScanService.processScanReport(report);

        assertEquals(0, created.size());
    }

    @Test
    void processScanReportRejectsRequestsWithoutAssetCoordinates() {
        ExternalScanService externalScanService = new ExternalScanService(
            mock(AssetRepository.class),
            mock(CveRepository.class),
            mock(TicketRepository.class),
            mock(UserRepository.class),
            new TicketService(
                mock(TicketRepository.class),
                mock(CveRepository.class),
                null,
                null,
                mock(UserRepository.class),
                new AuthorizationService(mock(AuthenticatedUserProvider.class))
            ),
            new ObjectMapper()
        );

        ScanReportDto report = new ScanReportDto();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> externalScanService.processScanReport(report));

        assertEquals(400, ex.getStatusCode().value());
    }
}
