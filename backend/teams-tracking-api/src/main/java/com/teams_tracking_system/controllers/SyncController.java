package com.teams_tracking_system.controllers;

import com.teams_tracking_system.dtos.schemas.AgentSyncResultResponse;
import com.teams_tracking_system.dtos.schemas.CheckInSyncResultResponse;
import com.teams_tracking_system.dtos.schemas.GeofenceSyncResultResponse;
import com.teams_tracking_system.dtos.schemas.LocationSyncResultResponse;
import com.teams_tracking_system.service.AgentSyncService;
import com.teams_tracking_system.service.CheckInSyncService;
import com.teams_tracking_system.service.GeofenceSyncService;
import com.teams_tracking_system.service.LocationSyncService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final AgentSyncService agentSyncService;
    private final CheckInSyncService checkInSyncService;
    private final LocationSyncService locationSyncService;
    private final GeofenceSyncService geofenceSyncService;

    public SyncController(
            AgentSyncService agentSyncService,
            CheckInSyncService checkInSyncService,
            LocationSyncService locationSyncService,
            GeofenceSyncService geofenceSyncService) {
        this.agentSyncService = agentSyncService;
        this.checkInSyncService = checkInSyncService;
        this.locationSyncService = locationSyncService;
        this.geofenceSyncService = geofenceSyncService;
    }

    @PostMapping("/agents")
    public AgentSyncResultResponse syncAgents(
            @RequestParam(required = false) Boolean active) {
        return agentSyncService.syncAgents(active);
    }

    @PostMapping("/check-ins")
    public CheckInSyncResultResponse syncCheckIns() {
        return checkInSyncService.syncCheckIns();
    }

    @PostMapping("/locations")
    public LocationSyncResultResponse syncLocations(
            @RequestParam(required = false) Boolean onlineOnly) {
        return locationSyncService.syncLocations(onlineOnly);
    }

    @PostMapping("/geofences")
    public GeofenceSyncResultResponse syncGeofences() {
        return geofenceSyncService.syncGeofences();
    }
}
