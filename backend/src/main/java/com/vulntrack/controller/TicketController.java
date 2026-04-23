package com.vulntrack.controller;

import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.TicketDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import com.vulntrack.entity.TicketPriority;
import com.vulntrack.entity.TicketStatus;
import com.vulntrack.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Validated
public class TicketController {

    private static final String CVE_ID_PATTERN = "^CVE-\\d{4}-\\d{4,7}$";

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<TicketDto> search(
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) TicketPriority priority,
        @RequestParam(required = false) Long assetId,
        @RequestParam(required = false) Long assigneeId,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ticketService.search(status, priority, assetId, assigneeId, q, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> stats() {
        return ticketService.getStats();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public TicketDto getById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TESTER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@RequestBody TicketDto.CreateRequest req) {
        return ticketService.create(req);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD','DEVELOPER')")
    public TicketDto transition(@PathVariable Long id, @RequestBody TicketDto.TransitionRequest req) {
        return ticketService.transition(id, req);
    }

    @PutMapping("/{id}/assignee")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD')")
    public TicketDto updateAssignee(@PathVariable Long id, @Valid @RequestBody TicketDto.AssigneeRequest req) {
        return ticketService.updateAssignee(id, req.getAssigneeId());
    }

    @PostMapping("/{id}/cves/{cveId}")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD')")
    public TicketDto addCve(@PathVariable Long id,
                            @PathVariable @Pattern(regexp = CVE_ID_PATTERN) String cveId) {
        return ticketService.addCve(id, cveId);
    }

    @DeleteMapping("/{id}/cves/{cveId}")
    @PreAuthorize("hasAnyRole('ADMIN','GROUP_LEAD')")
    public TicketDto removeCve(@PathVariable Long id,
                               @PathVariable @Pattern(regexp = CVE_ID_PATTERN) String cveId) {
        return ticketService.removeCve(id, cveId);
    }
}
