package com.teams_tracking_system.schedulers;

import com.teams_tracking_system.service.LocationSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.schedulers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LocationSyncScheduler extends NonConcurrentSyncScheduler {

    private final LocationSyncService locationSyncService;

    public LocationSyncScheduler(LocationSyncService locationSyncService) {
        super("Locations");
        this.locationSyncService = locationSyncService;
    }

    @Scheduled(
            initialDelayString = "${sync.schedulers.locations.initial-delay-ms:30000}",
            fixedDelayString = "${sync.schedulers.locations.fixed-delay-ms:60000}")
    public boolean runScheduledSync() {
        return runIfIdle(() -> locationSyncService.syncLocations(null));
    }
}
