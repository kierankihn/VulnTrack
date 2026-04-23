package com.vulntrack.service;

import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.*;
import com.vulntrack.repository.CveRepository;
import com.vulntrack.repository.TicketRepository;
import com.vulntrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepo;
    private final CveRepository cveRepo;
    private final AssetService assetService;
    private final UserService userService;
    private final UserRepository userRepo;
    private final AuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public PageResponse<TicketDto> search(TicketStatus status, TicketPriority priority,
                                          Long assetId, Long assigneeId, String q,
                                          int page, int size) {
        User actor = authorizationService.getCurrentUser();
        List<TicketDto> visible = ticketRepo.searchAll(status, priority, assetId, assigneeId, normalizeSearchPattern(q)).stream()
            .filter(ticket -> authorizationService.canViewTicket(actor, ticket))
            .map(ticket -> toDto(actor, ticket))
            .toList();
        return PageResponse.of(new PageImpl<>(
            paginate(visible, page, size),
            PageRequest.of(page, size),
            visible.size()
        ));
    }

    @Transactional(readOnly = true)
    public TicketDto findById(Long id) {
        Ticket ticket = getOrThrow(id);
        User actor = authorizationService.getCurrentUser();
        authorizationService.requireCanViewTicket(ticket);
        return toDto(actor, ticket);
    }

    public TicketDto create(TicketDto.CreateRequest req) {
        User actor = authorizationService.getCurrentUser();
        if (actor.getRole() != UserRole.ADMIN && actor.getRole() != UserRole.TESTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot create tickets");
        }
        if (actor.getRole() == UserRole.TESTER && req.getAssetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tester must select an asset");
        }
        Ticket t = new Ticket();
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setPriority(req.getPriority() != null ? req.getPriority() : TicketPriority.MEDIUM);
        t.setSource(TicketSource.MANUAL);

        if (req.getAssetId() != null) {
            Asset asset = assetService.getOrThrow(req.getAssetId());
            authorizationService.requireCanCreateTicket(asset);
            t.setAsset(asset);
        }
        if (actor.getRole() == UserRole.TESTER && req.getAssigneeId() != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tester cannot assign tickets during creation");
        }
        if (req.getAssigneeId() != null) {
            t.setAssignee(userService.getOrThrow(req.getAssigneeId()));
        }
        if (actor.getRole() == UserRole.TESTER) {
            t.setReporter(actor);
        } else if (req.getReporterId() != null) {
            t.setReporter(userService.getOrThrow(req.getReporterId()));
        }

        if (req.getCveIds() != null) {
            req.getCveIds().forEach(cveId ->
                cveRepo.findByCveId(cveId).ifPresent(cve -> t.getCves().add(cve))
            );
        }

        addHistoryEntry(t, "CREATED", null, TicketStatus.OPEN, null);
        return toDto(ticketRepo.save(t));
    }

    public TicketDto transition(Long id, TicketDto.TransitionRequest req) {
        Ticket t = getOrThrow(id);
        TicketStatus current = t.getStatus();
        TicketStatus target = req.getTargetStatus();
        authorizationService.requireCanTransitionTicket(t, target);

        if (!current.canTransitionTo(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot transition from " + current + " to " + target);
        }

        addHistoryEntry(t, "STATUS_CHANGED", current, target, req.getComment());

        t.setStatus(target);
        if (target == TicketStatus.RESOLVED || target == TicketStatus.CLOSED) {
            t.setResolvedAt(LocalDateTime.now());
        }
        return toDto(ticketRepo.save(t));
    }

    public TicketDto updateAssignee(Long id, Long assigneeId) {
        Ticket t = getOrThrow(id);
        User assignee = userService.getOrThrow(assigneeId);
        authorizationService.requireCanAssignTicket(t, assignee);
        t.setAssignee(assignee);
        addHistoryEntry(t, "REASSIGNED", null, null, "Assigned to: " + assignee.getFullName());
        return toDto(ticketRepo.save(t));
    }

    public TicketDto addCve(Long ticketId, String cveId) {
        Ticket t = getOrThrow(ticketId);
        authorizationService.requireCanViewTicket(t);
        CveEntry cve = cveRepo.findByCveId(cveId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CVE not found: " + cveId));
        t.getCves().add(cve);
        return toDto(ticketRepo.save(t));
    }

    public TicketDto removeCve(Long ticketId, String cveId) {
        Ticket t = getOrThrow(ticketId);
        authorizationService.requireCanViewTicket(t);
        t.getCves().removeIf(c -> c.getCveId().equals(cveId));
        return toDto(ticketRepo.save(t));
    }

    public Map<String, Object> getStats() {
        User actor = authorizationService.getCurrentUser();
        List<Ticket> visible = ticketRepo.searchAll(null, null, null, null, null).stream()
            .filter(ticket -> authorizationService.canViewTicket(actor, ticket))
            .toList();
        return Map.of(
            "open", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN).count(),
            "inProgress", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.IN_PROGRESS).count(),
            "inReview", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.IN_REVIEW).count(),
            "resolved", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED).count(),
            "closed", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.CLOSED).count(),
            "wontFix", visible.stream().filter(ticket -> ticket.getStatus() == TicketStatus.WONT_FIX).count(),
            "critical", visible.stream().filter(ticket -> ticket.getPriority() == TicketPriority.CRITICAL).count(),
            "high", visible.stream().filter(ticket -> ticket.getPriority() == TicketPriority.HIGH).count()
        );
    }

    public Ticket createFromScan(String title, String description, Asset asset, User assignee,
                                  TicketPriority priority, List<CveEntry> cves, String scanInfo) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription(description);
        t.setPriority(priority);
        t.setSource(TicketSource.AUTO_SCAN);
        t.setAsset(asset);
        t.setAssignee(assignee);
        t.setScanInfo(scanInfo);
        t.getCves().addAll(cves);
        addHistoryEntry(t, "CREATED_BY_SCAN", null, TicketStatus.OPEN, "Auto-created from CI/CD scan");
        return ticketRepo.save(t);
    }

    private void addHistoryEntry(Ticket t, String action, TicketStatus from, TicketStatus to, String note) {
        String entry = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
            + " | " + action
            + (from != null ? " | FROM:" + from : "")
            + (to != null ? " | TO:" + to : "")
            + (note != null && !note.isBlank() ? " | " + note : "");
        t.getStatusHistory().add(entry);
    }

    public Ticket getOrThrow(Long id) {
        return ticketRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found: " + id));
    }

    private String normalizeSearchPattern(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private TicketDto toDto(Ticket ticket) {
        return toDto(authorizationService.getCurrentUser(), ticket);
    }

    private TicketDto toDto(User actor, Ticket ticket) {
        return TicketDto.from(ticket, authorizationService.allowedTransitions(actor, ticket));
    }
}
