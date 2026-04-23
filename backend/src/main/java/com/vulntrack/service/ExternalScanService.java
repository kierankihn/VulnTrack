package com.vulntrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.dto.ScanReportDto;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.*;
import com.vulntrack.repository.AssetRepository;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.TicketRepository;
import com.vulntrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExternalScanService {

    private final AssetRepository assetRepo;
    private final CveRepository cveRepo;
    private final TicketRepository ticketRepo;
    private final UserRepository userRepo;
    private final TicketService ticketService;
    private final ObjectMapper objectMapper;

    public List<TicketDto> processScanReport(ScanReportDto report) {
        Asset asset = resolveAsset(report);

        Map<String, List<ScanReportDto.Vulnerability>> cveToVulns = new HashMap<>();
        for (ScanReportDto.Dependency dep : report.getDependencies()) {
            if (dep.getVulnerabilities() == null) continue;
            for (ScanReportDto.Vulnerability vuln : dep.getVulnerabilities()) {
                if (vuln.getName() != null && vuln.getName().startsWith("CVE-")) {
                    cveToVulns.computeIfAbsent(vuln.getName(), k -> new ArrayList<>()).add(vuln);
                }
            }
        }

        List<TicketDto> created = new ArrayList<>();
        for (Map.Entry<String, List<ScanReportDto.Vulnerability>> entry : cveToVulns.entrySet()) {
            String cveId = entry.getKey();

            if (ticketRepo.existsOpenTicketForCveAndAsset(cveId, asset.getId())) {
                log.info("Skipping duplicate: {} for asset {}", cveId, asset.getId());
                continue;
            }

            Optional<CveEntry> cveOpt = cveRepo.findByCveId(cveId);
            List<CveEntry> cves = new ArrayList<>();
            cveOpt.ifPresent(cves::add);

            ScanReportDto.Vulnerability representative = entry.getValue().get(0);
            TicketPriority priority = determinePriority(representative, cveOpt.orElse(null));
            User assignee = autoAssign(asset);

            String title = "[SCAN] " + cveId + " in " + asset.getName();
            String description = buildDescription(cveId, entry.getValue(), report.getProjectName());
            String scanInfo = buildScanInfo(entry.getValue(), report);

            Ticket ticket = ticketService.createFromScan(title, description, asset, assignee,
                priority, cves, scanInfo);
            created.add(TicketDto.from(ticket));
            log.info("Created ticket {} for {} on asset {}", ticket.getId(), cveId, asset.getName());
        }
        return created;
    }

    private Asset resolveAsset(ScanReportDto report) {
        if (report.getAssetId() != null) {
            return assetRepo.findById(report.getAssetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Asset not found: " + report.getAssetId()));
        }
        if (report.getProjectName() != null) {
            return assetRepo.findByProjectName(report.getProjectName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No asset registered for project: " + report.getProjectName()));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "assetId or projectName required");
    }

    private User autoAssign(Asset asset) {
        if (asset.getDevGroup() == null) return null;
        List<User> candidates = userRepo.findLeastLoadedMemberInGroup(asset.getDevGroup().getId());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private TicketPriority determinePriority(ScanReportDto.Vulnerability vuln, CveEntry cve) {
        double score = 0;
        if (cve != null && cve.getCvssV3Score() != null) {
            score = cve.getCvssV3Score();
        } else if (vuln.getCvssScore() != null) {
            score = vuln.getCvssScore();
        } else if (vuln.getSeverity() != null) {
            return switch (vuln.getSeverity().toUpperCase()) {
                case "CRITICAL" -> TicketPriority.CRITICAL;
                case "HIGH" -> TicketPriority.HIGH;
                case "MEDIUM" -> TicketPriority.MEDIUM;
                default -> TicketPriority.LOW;
            };
        }
        if (score >= 9.0) return TicketPriority.CRITICAL;
        if (score >= 7.0) return TicketPriority.HIGH;
        if (score >= 4.0) return TicketPriority.MEDIUM;
        return TicketPriority.LOW;
    }

    private String buildDescription(String cveId, List<ScanReportDto.Vulnerability> vulns, String project) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(cveId).append("\n\n");
        sb.append("**Project:** ").append(project).append("\n\n");
        ScanReportDto.Vulnerability v = vulns.get(0);
        if (v.getDescription() != null) {
            sb.append("**Description:** ").append(v.getDescription()).append("\n\n");
        }
        sb.append("**Affected files:**\n");
        vulns.forEach(vuln -> sb.append("- ").append(vuln.getDescription()).append("\n"));
        return sb.toString();
    }

    private String buildScanInfo(List<ScanReportDto.Vulnerability> vulns, ScanReportDto report) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "project", report.getProjectName(),
                "repoUrl", report.getRepoUrl() != null ? report.getRepoUrl() : "",
                "vulnCount", vulns.size()
            ));
        } catch (Exception e) {
            return "{}";
        }
    }
}
