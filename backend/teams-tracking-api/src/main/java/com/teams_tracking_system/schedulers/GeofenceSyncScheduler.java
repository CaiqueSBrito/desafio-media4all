package com.teams_tracking_system.schedulers;

import com.teams_tracking_system.service.GeofenceSyncService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sync.schedulers", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GeofenceSyncScheduler extends NonConcurrentSyncScheduler {

    private final GeofenceSyncService geofenceSyncService;

    public GeofenceSyncScheduler(GeofenceSyncService geofenceSyncService) {
        super("Geofences");
        this.geofenceSyncService = geofenceSyncService;
    }

    @Scheduled(
            initialDelayString = "${sync.schedulers.geofences.initial-delay-ms:30000}",
            fixedDelayString = "${sync.schedulers.geofences.fixed-delay-ms:600000}")
    public boolean runScheduledSync() {
        return runIfIdle(geofenceSyncService::syncGeofences);
    }
}
