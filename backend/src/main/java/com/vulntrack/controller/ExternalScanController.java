package com.vulntrack.controller;

import com.vulntrack.dto.ScanReportDto;
import com.vulntrack.dto.TicketDto;
import com.vulntrack.service.ExternalScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
public class ExternalScanController {

    private final ExternalScanService externalScanService;

    @Value("${external.api.key:}")
    private String expectedApiKey;

    @PostMapping("/scan")
    public ResponseEntity<?> receiveScanReport(
        @RequestHeader(value = "X-API-Key", required = false) String apiKey,
        @RequestBody ScanReportDto report
    ) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid API key"));
        }

        List<TicketDto> created = externalScanService.processScanReport(report);
        return ResponseEntity.ok(Map.of(
            "message", "Scan processed successfully",
            "ticketsCreated", created.size(),
            "tickets", created
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "vulntrack-external-api"));
    }

    private boolean isValidApiKey(String provided) {
        if (expectedApiKey == null || expectedApiKey.isBlank() || provided == null) {
            return false;
        }
        byte[] a = expectedApiKey.getBytes(StandardCharsets.UTF_8);
        byte[] b = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
