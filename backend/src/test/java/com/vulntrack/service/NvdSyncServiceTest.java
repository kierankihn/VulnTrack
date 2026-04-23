package com.vulntrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NvdSyncServiceTest {

    @Test
    void buildSyncUrlUsesLastModifiedWindowForIncrementalSync() {
        NvdSyncService service = new NvdSyncService(null, new ObjectMapper(), systemSettingService());
        ReflectionTestUtils.setField(service, "nvdBaseUrl", "https://services.nvd.nist.gov/rest/json/cves/2.0");

        String url = service.buildSyncUrl(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 21), 4000);

        assertEquals(
            "https://services.nvd.nist.gov/rest/json/cves/2.0"
                + "?lastModStartDate=2026-04-01T00:00:00.000"
                + "&lastModEndDate=2026-04-21T23:59:59.999"
                + "&startIndex=4000"
                + "&resultsPerPage=2000",
            url
        );
    }

    @Test
    void resolveApiKeyReadsOnlyPersistedSystemSetting() {
        NvdSyncService service = new NvdSyncService(null, new ObjectMapper(), systemSettingService());

        String apiKey = ReflectionTestUtils.invokeMethod(service, "resolveApiKey");

        assertEquals("", apiKey);
    }

    private SystemSettingService systemSettingService() {
        com.vulntrack.repository.SystemSettingRepository repo = mock(com.vulntrack.repository.SystemSettingRepository.class);
        when(repo.findById(SystemSettingService.NVD_API_KEY)).thenReturn(Optional.empty());
        return new SystemSettingService(repo);
    }
}
