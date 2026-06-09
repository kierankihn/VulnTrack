package com.vulntrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vulntrack.entity.CveEntry;
import com.vulntrack.entity.CveSeverity;
import com.vulntrack.repository.CveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NvdApiMirrorServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildsDependencyCheckCompatibleNvdResponseFromLocalCves() {
        CveRepository cveRepository = mock(CveRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        NvdApiMirrorService service = new NvdApiMirrorService(cveRepository, objectMapper);

        CveEntry entry = new CveEntry();
        entry.setCveId("CVE-2026-12345");
        entry.setDescription("Example issue");
        entry.setSeverity(CveSeverity.HIGH);
        entry.setCvssV3Score(8.8);
        entry.setCvssV3Vector("CVSS:3.1/AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H");
        entry.setPublishedDate(LocalDate.of(2026, 6, 1));
        entry.setLastModifiedDate(LocalDate.of(2026, 6, 2));
        entry.setAffectedProducts("""
            [{"nodes":[{"cpeMatch":[{"vulnerable":true,"criteria":"cpe:2.3:a:example:lib:1.0:*:*:*:*:*:*:*"}]}]}]
            """);
        entry.setReferences("https://example.test/advisory\nhttps://example.test/patch");

        when(cveRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entry), Pageable.ofSize(1), 1));

        Map<String, Object> response = service.getCves(new NvdApiMirrorService.Request(
            0,
            1,
            null,
            null,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 30),
            null,
            null
        ));

        assertEquals(1, response.get("resultsPerPage"));
        assertEquals(0, response.get("startIndex"));
        assertEquals(1L, response.get("totalResults"));
        assertEquals("NVD_CVE", response.get("format"));
        assertEquals("2.0", response.get("version"));

        List<Map<String, Object>> vulnerabilities = (List<Map<String, Object>>) response.get("vulnerabilities");
        Map<String, Object> cve = (Map<String, Object>) vulnerabilities.getFirst().get("cve");
        assertEquals("CVE-2026-12345", cve.get("id"));
        assertEquals("2026-06-01T00:00:00.000", cve.get("published"));
        assertEquals("2026-06-02T00:00:00.000", cve.get("lastModified"));

        JsonNode cveJson = objectMapper.valueToTree(cve);
        assertEquals("Example issue", cveJson.path("descriptions").get(0).path("value").asText());
        assertEquals(8.8, cveJson.path("metrics").path("cvssMetricV31").get(0).path("cvssData").path("baseScore").asDouble());
        assertEquals("HIGH", cveJson.path("metrics").path("cvssMetricV31").get(0).path("cvssData").path("baseSeverity").asText());
        assertEquals("cpe:2.3:a:example:lib:1.0:*:*:*:*:*:*:*",
            cveJson.path("configurations").get(0).path("nodes").get(0).path("cpeMatch").get(0).path("criteria").asText());
        assertEquals("https://example.test/patch", cveJson.path("references").get(1).path("url").asText());
        assertNotNull(response.get("timestamp"));
    }
}
