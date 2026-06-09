package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import com.vulntrack.repository.CveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NvdSyncService {

    private final CveRepository cveRepo;
    private final ObjectMapper objectMapper;
    private final SystemSettingService systemSettingService;

    @Value("${nvd.api.base-url}")
    private String nvdBaseUrl;

    @Value("${nvd.api.sync-days-back:7}")
    private int syncDaysBack;

    private static final int PAGE_SIZE = 2000;
    private static final int MAX_DATE_RANGE_DAYS = 120;

    // Allowlist of hosts we will contact during NVD sync. Prevents SSRF via a
    // misconfigured or attacker-controlled nvd.api.base-url value.
    private static final Set<String> ALLOWED_NVD_HOSTS = Set.of("services.nvd.nist.gov");

    @Transactional
    public SyncResult sync() {
        SyncResult result = new SyncResult();

        try {
            RestClient client = buildClient();
            int remoteTotal = fetchRemoteTotalResults(client);
            long localTotal = cveRepo.count();

            if (requiresBackfill(localTotal, remoteTotal)) {
                log.info("Starting full NVD backfill: local {} of remote {}", localTotal, remoteTotal);
                syncAll(client, result);
            } else {
                LocalDate since = cveRepo.findTopByOrderByLastModifiedDateDesc()
                    .map(CveEntry::getLastModifiedDate)
                    .map(d -> d.minusDays(1))
                    .orElse(LocalDate.now().minusDays(syncDaysBack));

                log.info("Starting incremental NVD sync from {}", since);
                syncIncrementalWindows(client, since, LocalDate.now(), result);
            }
            log.info("NVD sync complete: {} added, {} updated", result.added, result.updated);
        } catch (Exception e) {
            log.error("NVD sync failed", e);
            result.error = e.getMessage();
        }
        return result;
    }

    private int fetchRemoteTotalResults(RestClient client) throws Exception {
        String url = buildTotalResultsUrl();
        validateNvdUrl(url);
        String body = client.get().uri(url).retrieve().body(String.class);
        return objectMapper.readTree(body).path("totalResults").asInt(0);
    }

    private void syncAll(RestClient client, SyncResult result) throws Exception {
        syncPages(client, startIndex -> buildFullSyncUrl(startIndex), result);
    }

    private void syncIncrementalWindows(RestClient client, LocalDate from, LocalDate to, SyncResult result) throws Exception {
        LocalDate windowStart = from;
        while (!windowStart.isAfter(to)) {
            LocalDate windowEnd = windowStart.plusDays(MAX_DATE_RANGE_DAYS - 1L);
            if (windowEnd.isAfter(to)) {
                windowEnd = to;
            }
            syncRange(client, windowStart, windowEnd, result);
            windowStart = windowEnd.plusDays(1);
        }
    }

    private void syncRange(RestClient client, LocalDate from, LocalDate to, SyncResult result) throws Exception {
        syncPages(client, startIndex -> buildSyncUrl(from, to, startIndex), result);
    }

    private void syncPages(RestClient client, PageUrlBuilder urlBuilder, SyncResult result) throws Exception {
        int startIndex = 0;
        int totalResults;

        do {
            String url = urlBuilder.build(startIndex);
            validateNvdUrl(url);

            log.debug("Fetching NVD: {}", url);
            String body = client.get().uri(url).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);

            totalResults = root.path("totalResults").asInt(0);
            JsonNode vulns = root.path("vulnerabilities");

            List<CveEntry> batch = new ArrayList<>();
            for (JsonNode item : vulns) {
                try {
                    CveEntry entry = parseVulnerability(item.path("cve"));
                    batch.add(entry);
                } catch (Exception e) {
                    log.warn("Failed to parse CVE entry: {}", e.getMessage());
                }
            }

            saveBatch(batch, result);
            startIndex += PAGE_SIZE;

            if (startIndex < totalResults) {
                Thread.sleep(600);
            }
        } while (startIndex < totalResults);
    }

    String buildFullSyncUrl(int startIndex) {
        return nvdBaseUrl
            + "?startIndex=" + startIndex
            + "&resultsPerPage=" + PAGE_SIZE;
    }

    String buildTotalResultsUrl() {
        return nvdBaseUrl
            + "?startIndex=0"
            + "&resultsPerPage=1";
    }

    boolean requiresBackfill(long localCount, int remoteTotal) {
        return remoteTotal > 0 && localCount < remoteTotal;
    }

    String buildSyncUrl(LocalDate from, LocalDate to, int startIndex) {
        String lastModStartDate = from + "T00:00:00.000";
        String lastModEndDate = to + "T23:59:59.999";
        return nvdBaseUrl + "?lastModStartDate=" + lastModStartDate
            + "&lastModEndDate=" + lastModEndDate
            + "&startIndex=" + startIndex
            + "&resultsPerPage=" + PAGE_SIZE;
    }

    private CveEntry parseVulnerability(JsonNode cve) {
        String cveId = cve.path("id").asText();
        CveEntry entry = cveRepo.findByCveId(cveId).orElse(new CveEntry());
        entry.setCveId(cveId);

        JsonNode descriptions = cve.path("descriptions");
        for (JsonNode desc : descriptions) {
            if ("en".equals(desc.path("lang").asText())) {
                entry.setDescription(desc.path("value").asText());
                break;
            }
        }

        String publishedStr = cve.path("published").asText(null);
        if (publishedStr != null) {
            entry.setPublishedDate(LocalDate.parse(publishedStr.substring(0, 10)));
        }

        String modifiedStr = cve.path("lastModified").asText(null);
        if (modifiedStr != null) {
            entry.setLastModifiedDate(LocalDate.parse(modifiedStr.substring(0, 10)));
        }

        JsonNode metrics = cve.path("metrics");
        JsonNode cvssV3 = metrics.path("cvssMetricV31");
        if (cvssV3.isMissingNode()) cvssV3 = metrics.path("cvssMetricV30");
        if (!cvssV3.isMissingNode() && cvssV3.isArray() && cvssV3.size() > 0) {
            JsonNode cvssData = cvssV3.get(0).path("cvssData");
            entry.setCvssV3Score(cvssData.path("baseScore").asDouble(0));
            entry.setCvssV3Vector(cvssData.path("vectorString").asText(null));
            entry.setSeverity(parseSeverity(cvssData.path("baseSeverity").asText("")));
        } else {
            JsonNode cvssV2 = metrics.path("cvssMetricV2");
            if (!cvssV2.isMissingNode() && cvssV2.isArray() && cvssV2.size() > 0) {
                JsonNode cvssData = cvssV2.get(0).path("cvssData");
                entry.setCvssV2Score(cvssData.path("baseScore").asDouble(0));
                entry.setSeverity(scoreToSeverity(entry.getCvssV2Score()));
            }
        }

        JsonNode configs = cve.path("configurations");
        if (!configs.isMissingNode()) {
            entry.setAffectedProducts(configs.toString());
        }

        JsonNode refs = cve.path("references");
        if (!refs.isMissingNode()) {
            List<String> refUrls = new ArrayList<>();
            for (JsonNode ref : refs) {
                refUrls.add(ref.path("url").asText());
            }
            entry.setReferences(String.join("\n", refUrls));
        }

        entry.setSyncedAt(LocalDateTime.now());
        return entry;
    }

    private void saveBatch(List<CveEntry> batch, SyncResult result) {
        for (CveEntry e : batch) {
            boolean isNew = e.getId() == null;
            cveRepo.save(e);
            if (isNew) result.added++;
            else result.updated++;
        }
    }

    private RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        RestClient.Builder builder = RestClient.builder()
            .requestFactory(factory)
            .defaultHeader("Accept", "application/json");
        String apiKey = resolveApiKey();
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("apiKey", apiKey);
        }
        return builder.build();
    }

    private void validateNvdUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(scheme) || host == null || !ALLOWED_NVD_HOSTS.contains(host)) {
                throw new IllegalArgumentException("Refusing to call non-allowlisted NVD host: " + host);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid NVD URL: " + url, ex);
        }
    }

    String resolveApiKey() {
        return systemSettingService.get(SystemSettingService.NVD_API_KEY, "");
    }

    private CveSeverity parseSeverity(String s) {
        return switch (s.toUpperCase()) {
            case "CRITICAL" -> CveSeverity.CRITICAL;
            case "HIGH"     -> CveSeverity.HIGH;
            case "MEDIUM"   -> CveSeverity.MEDIUM;
            case "LOW"      -> CveSeverity.LOW;
            default         -> CveSeverity.NONE;
        };
    }

    private CveSeverity scoreToSeverity(double score) {
        if (score >= 9.0) return CveSeverity.CRITICAL;
        if (score >= 7.0) return CveSeverity.HIGH;
        if (score >= 4.0) return CveSeverity.MEDIUM;
        if (score > 0.0)  return CveSeverity.LOW;
        return CveSeverity.NONE;
    }

    @FunctionalInterface
    private interface PageUrlBuilder {
        String build(int startIndex);
    }

    @lombok.Data
    public static class SyncResult {
        int added;
        int updated;
        String error;
    }
}
