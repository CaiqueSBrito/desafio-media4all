package com.teams_tracking_system.schedulers;

import com.teams_tracking_system.service.CheckInSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.schedulers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CheckInSyncScheduler extends NonConcurrentSyncScheduler {

    private final CheckInSyncService checkInSyncService;

    public CheckInSyncScheduler(CheckInSyncService checkInSyncService) {
        super("Check-ins");
        this.checkInSyncService = checkInSyncService;
    }

    @Scheduled(
            initialDelayString = "${sync.schedulers.check-ins.initial-delay-ms:30000}",
            fixedDelayString = "${sync.schedulers.check-ins.fixed-delay-ms:60000}")
    public boolean runScheduledSync() {
        return runIfIdle(checkInSyncService::syncCheckIns);
    }
}
