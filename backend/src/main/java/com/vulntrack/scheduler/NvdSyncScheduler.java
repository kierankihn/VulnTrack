package com.vulntrack.scheduler;

import com.vulntrack.service.NvdSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NvdSyncScheduler {

    private final NvdSyncService nvdSyncService;

    // Run daily at 02:00 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduledSync() {
        log.info("Scheduled NVD sync starting");
        nvdSyncService.sync();
    }
}
