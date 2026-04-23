package com.vulntrack.service;

import com.vulntrack.dto.CveDto;
import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import com.vulntrack.entity.User;
import com.vulntrack.entity.UserRole;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CveService {

    private final CveRepository cveRepo;
    private final TicketRepository ticketRepo;
    private final AuthorizationService authorizationService;

    public PageResponse<CveDto> search(String q, CveSeverity severity, Double minScore, int page, int size) {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN) {
            List<CveDto> visible = cveRepo.findAll().stream()
                .filter(cve -> matchesQuery(cve, q, severity, minScore))
                .filter(cve -> isVisibleTo(actor, cve.getId()))
                .sorted(Comparator.comparing(CveEntry::getPublishedDate).reversed())
                .map(CveDto::from)
                .toList();
            return PageResponse.of(new PageImpl<>(
                paginate(visible, page, size),
                PageRequest.of(page, size),
                visible.size()
            ));
        }
        return PageResponse.of(
            cveRepo.search(normalizeSearchPattern(q), severity, minScore, PageRequest.of(page, size)).map(CveDto::from)
        );
    }

    public CveDto findById(Long id) {
        CveEntry cveEntry = getOrThrow(id);
        requireVisible(cveEntry);
        return CveDto.from(cveEntry);
    }

    public CveDto findByCveId(String cveId) {
        CveEntry cveEntry = cveRepo.findByCveId(cveId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE not found: " + cveId));
        requireVisible(cveEntry);
        return CveDto.from(cveEntry);
    }

    public List<TicketDto> getRelatedTickets(Long id) {
        getOrThrow(id);
        User actor = authorizationService.getCurrentUser();
        return ticketRepo.findByCveId(id).stream()
            .filter(ticket -> authorizationService.canViewTicket(actor, ticket))
            .map(ticket -> TicketDto.from(ticket, authorizationService.allowedTransitions(actor, ticket)))
            .toList();
    }

    public Map<String, Object> getStats() {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN) {
            List<CveEntry> visible = cveRepo.findAll().stream()
                .filter(cve -> isVisibleTo(actor, cve.getId()))
                .toList();
            return Map.of(
                "total", visible.size(),
                "critical", visible.stream().filter(cve -> cve.getSeverity() == CveSeverity.CRITICAL).count(),
                "high", visible.stream().filter(cve -> cve.getSeverity() == CveSeverity.HIGH).count(),
                "medium", visible.stream().filter(cve -> cve.getSeverity() == CveSeverity.MEDIUM).count(),
                "low", visible.stream().filter(cve -> cve.getSeverity() == CveSeverity.LOW).count()
            );
        }
        return Map.of(
            "total", cveRepo.count(),
            "critical", cveRepo.countBySeverity(CveSeverity.CRITICAL),
            "high", cveRepo.countBySeverity(CveSeverity.HIGH),
            "medium", cveRepo.countBySeverity(CveSeverity.MEDIUM),
            "low", cveRepo.countBySeverity(CveSeverity.LOW)
        );
    }

    public CveEntry getOrThrow(Long id) {
        return cveRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE not found: " + id));
    }

    public CveEntry getOrThrowByCveId(String cveId) {
        return cveRepo.findByCveId(cveId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE not found: " + cveId));
    }

    private String normalizeSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private boolean matchesQuery(CveEntry cve, String q, CveSeverity severity, Double minScore) {
        boolean qMatches = q == null || q.isBlank()
            || (cve.getCveId() != null && cve.getCveId().toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT)))
            || (cve.getDescription() != null && cve.getDescription().toLowerCase(Locale.ROOT).contains(q.trim().toLowerCase(Locale.ROOT)));
        boolean severityMatches = severity == null || severity == cve.getSeverity();
        boolean scoreMatches = minScore == null || (cve.getCvssV3Score() != null && cve.getCvssV3Score() >= minScore);
        return qMatches && severityMatches && scoreMatches;
    }

    private boolean isVisibleTo(User actor, Long cveId) {
        return ticketRepo.findByCveId(cveId).stream()
            .anyMatch(ticket -> authorizationService.canViewTicket(actor, ticket));
    }

    private void requireVisible(CveEntry cveEntry) {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!isVisibleTo(actor, cveEntry.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE not found: " + cveEntry.getId());
        }
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
