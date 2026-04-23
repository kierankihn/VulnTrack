package com.vulntrack.controller;

import com.vulntrack.dto.CveDto;
import com.vulntrack.dto.PageResponse;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.entity.CveSeverity;
import com.vulntrack.service.CveService;
import com.vulntrack.service.NvdSyncService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cves")
@RequiredArgsConstructor
@Validated
public class CveController {

    private static final String CVE_ID_PATTERN = "^CVE-\\d{4}-\\d{4,7}$";

    private final CveService cveService;
    private final NvdSyncService nvdSyncService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PageResponse<CveDto> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) CveSeverity severity,
        @RequestParam(required = false) Double minScore,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return cveService.search(q, severity, minScore, page, size);
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> stats() {
        return cveService.getStats();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public CveDto getById(@PathVariable Long id) {
        return cveService.findById(id);
    }

    @GetMapping("/cve/{cveId}")
    @PreAuthorize("isAuthenticated()")
    public CveDto getByCveId(@PathVariable @Pattern(regexp = CVE_ID_PATTERN) String cveId) {
        return cveService.findByCveId(cveId);
    }

    @GetMapping("/{id}/tickets")
    @PreAuthorize("isAuthenticated()")
    public List<TicketDto> getRelatedTickets(@PathVariable Long id) {
        return cveService.getRelatedTickets(id);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NvdSyncService.SyncResult> triggerSync() {
        NvdSyncService.SyncResult result = nvdSyncService.sync();
        return ResponseEntity.ok(result);
    }
}
