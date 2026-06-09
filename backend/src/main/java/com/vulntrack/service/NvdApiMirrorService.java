package com.vulntrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import com.vulntrack.repository.CveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NvdApiMirrorService {

    private static final int DEFAULT_RESULTS_PER_PAGE = 2000;
    private static final int MAX_RESULTS_PER_PAGE = 2000;
    private static final DateTimeFormatter NVD_LOCAL_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final CveRepository cveRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getCves(Request request) {
        int startIndex = Math.max(0, request.startIndex());
        int resultsPerPage = normalizeResultsPerPage(request.resultsPerPage());
        Pageable pageable = new OffsetPageRequest(
            startIndex,
            resultsPerPage,
            Sort.by(Sort.Order.asc("publishedDate"), Sort.Order.asc("cveId"))
        );

        Page<CveEntry> page = cveRepository.findAll(toSpecification(request), pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("resultsPerPage", page.getNumberOfElements());
        response.put("startIndex", startIndex);
        response.put("totalResults", page.getTotalElements());
        response.put("format", "NVD_CVE");
        response.put("version", "2.0");
        response.put("timestamp", Instant.now().toString());
        response.put("vulnerabilities", page.getContent().stream()
            .map(this::toVulnerability)
            .toList());
        return response;
    }

    private int normalizeResultsPerPage(int requested) {
        if (requested <= 0) {
            return DEFAULT_RESULTS_PER_PAGE;
        }
        return Math.min(requested, MAX_RESULTS_PER_PAGE);
    }

    private Specification<CveEntry> toSpecification(Request request) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (request.lastModStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastModifiedDate"), request.lastModStartDate()));
            }
            if (request.lastModEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastModifiedDate"), request.lastModEndDate()));
            }
            if (request.pubStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishedDate"), request.pubStartDate()));
            }
            if (request.pubEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishedDate"), request.pubEndDate()));
            }
            if (request.cveIds() != null && !request.cveIds().isEmpty()) {
                predicates.add(root.get("cveId").in(request.cveIds()));
            }
            if (request.cvssV3Severity() != null) {
                predicates.add(cb.equal(root.get("severity"), request.cvssV3Severity()));
            }

            return predicates.isEmpty()
                ? cb.conjunction()
                : cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Map<String, Object> toVulnerability(CveEntry entry) {
        return Map.of("cve", toCve(entry));
    }

    private Map<String, Object> toCve(CveEntry entry) {
        Map<String, Object> cve = new LinkedHashMap<>();
        cve.put("id", entry.getCveId());
        cve.put("sourceIdentifier", "vulntrack.local");
        cve.put("published", formatDate(entry.getPublishedDate()));
        cve.put("lastModified", formatDate(entry.getLastModifiedDate()));
        cve.put("vulnStatus", "Analyzed");
        cve.put("descriptions", List.of(Map.of(
            "lang", "en",
            "value", entry.getDescription() == null ? "" : entry.getDescription()
        )));
        cve.put("metrics", metrics(entry));
        cve.put("weaknesses", List.of());
        cve.put("configurations", parseJson(entry.getAffectedProducts()));
        cve.put("references", references(entry.getReferences()));
        return cve;
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay().format(NVD_LOCAL_DATE_TIME);
    }

    private Map<String, Object> metrics(CveEntry entry) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        if (entry.getCvssV3Score() != null || entry.getCvssV3Vector() != null) {
            Map<String, Object> cvssData = new LinkedHashMap<>();
            cvssData.put("version", "3.1");
            cvssData.put("vectorString", entry.getCvssV3Vector());
            cvssData.put("baseScore", entry.getCvssV3Score() == null ? 0.0 : entry.getCvssV3Score());
            cvssData.put("baseSeverity", entry.getSeverity() == null ? CveSeverity.NONE.name() : entry.getSeverity().name());
            metrics.put("cvssMetricV31", List.of(Map.of(
                "source", "vulntrack.local",
                "type", "Primary",
                "cvssData", cvssData
            )));
        }
        if (entry.getCvssV2Score() != null) {
            Map<String, Object> cvssData = new LinkedHashMap<>();
            cvssData.put("version", "2.0");
            cvssData.put("baseScore", entry.getCvssV2Score());
            metrics.put("cvssMetricV2", List.of(Map.of(
                "source", "vulntrack.local",
                "type", "Primary",
                "cvssData", cvssData,
                "baseSeverity", entry.getSeverity() == null ? CveSeverity.NONE.name() : entry.getSeverity().name()
            )));
        }
        return metrics;
    }

    private Object parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<Map<String, String>> references(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split("\\R"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(url -> Map.of("url", url, "source", "vulntrack.local"))
            .toList();
    }

    public record Request(
        int startIndex,
        int resultsPerPage,
        LocalDate lastModStartDate,
        LocalDate lastModEndDate,
        LocalDate pubStartDate,
        LocalDate pubEndDate,
        List<String> cveIds,
        CveSeverity cvssV3Severity
    ) {
    }

    private record OffsetPageRequest(int offset, int pageSize, Sort sort) implements Pageable {

        private OffsetPageRequest {
            if (offset < 0) {
                throw new IllegalArgumentException("offset must not be negative");
            }
            if (pageSize < 1) {
                throw new IllegalArgumentException("pageSize must be greater than zero");
            }
            sort = Objects.requireNonNullElse(sort, Sort.unsorted());
        }

        @Override
        public int getPageNumber() {
            return offset / pageSize;
        }

        @Override
        public int getPageSize() {
            return pageSize;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new OffsetPageRequest(offset + pageSize, pageSize, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            return hasPrevious() ? new OffsetPageRequest(Math.max(offset - pageSize, 0), pageSize, sort) : first();
        }

        @Override
        public Pageable first() {
            return new OffsetPageRequest(0, pageSize, sort);
        }

        @Override
        public Pageable withPage(int pageNumber) {
            if (pageNumber < 0) {
                throw new IllegalArgumentException("pageNumber must not be negative");
            }
            return new OffsetPageRequest(pageNumber * pageSize, pageSize, sort);
        }

        @Override
        public boolean hasPrevious() {
            return offset > 0;
        }
    }
}
