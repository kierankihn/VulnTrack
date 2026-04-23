package com.vulntrack.controller;

import com.vulntrack.service.SystemSettingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SystemSettingService settings;

    @GetMapping("/nvd-api-key")
    @PreAuthorize("hasRole('ADMIN')")
    public NvdKeyResponse getNvdKey() {
        String raw = settings.get(SystemSettingService.NVD_API_KEY, "");
        NvdKeyResponse resp = new NvdKeyResponse();
        resp.setConfigured(raw != null && !raw.isBlank());
        resp.setMaskedValue(mask(raw));
        return resp;
    }

    @PutMapping("/nvd-api-key")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setNvdKey(@Valid @RequestBody NvdKeyRequest req) {
        settings.set(SystemSettingService.NVD_API_KEY, req.getValue() == null ? "" : req.getValue().trim());
    }

    private String mask(String raw) {
        if (raw == null || raw.isBlank()) return "";
        int n = raw.length();
        if (n <= 8) return "•".repeat(n);
        return raw.substring(0, 4) + "••••" + raw.substring(n - 4);
    }

    @Data
    public static class NvdKeyRequest {
        @NotNull
        private String value;
    }

    @Data
    public static class NvdKeyResponse {
        private boolean configured;
        private String maskedValue;
    }
}
