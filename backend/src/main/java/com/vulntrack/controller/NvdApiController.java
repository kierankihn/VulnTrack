package com.vulntrack.controller;

import com.vulntrack.entity.CveSeverity;
import com.vulntrack.service.NvdApiMirrorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping({
    "/api/v1/nvd/rest/json/cves/2.0",
    "/api/v1/nvd/cves/2.0"
})
@RequiredArgsConstructor
public class NvdApiController {

    private final NvdApiMirrorService nvdApiMirrorService;

    @GetMapping
    public Map<String, Object> getCves(
        @RequestParam(defaultValue = "0") int startIndex,
        @RequestParam(defaultValue = "2000") int resultsPerPage,
        @RequestParam(required = false) String lastModStartDate,
        @RequestParam(required = false) String lastModEndDate,
        @RequestParam(required = false) String pubStartDate,
        @RequestParam(required = false) String pubEndDate,
        @RequestParam(required = false) String cveId,
        @RequestParam(required = false) String cveIds,
        @RequestParam(required = false) String cvssV3Severity
    ) {
        return nvdApiMirrorService.getCves(new NvdApiMirrorService.Request(
            startIndex,
            resultsPerPage,
            parseDate(lastModStartDate),
            parseDate(lastModEndDate),
            parseDate(pubStartDate),
            parseDate(pubEndDate),
            parseCveIds(cveId, cveIds),
            parseSeverity(cvssV3Severity)
        ));
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private List<String> parseCveIds(String cveId, String cveIds) {
        String raw = cveIds != null && !cveIds.isBlank() ? cveIds : cveId;
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(s -> s.toUpperCase(Locale.ROOT))
            .toList();
    }

    private CveSeverity parseSeverity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return CveSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
