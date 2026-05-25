package com.teams_tracking_system.controllers;

import com.teams_tracking_system.dtos.schemas.SyncMonitoringResponse;
import com.teams_tracking_system.service.SyncMonitoringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring")
public class SyncMonitoringController {

    private final SyncMonitoringService syncMonitoringService;

    public SyncMonitoringController(SyncMonitoringService syncMonitoringService) {
        this.syncMonitoringService = syncMonitoringService;
    }

    @GetMapping("/sync")
    public SyncMonitoringResponse getSyncMonitoring(
            @RequestParam(required = false) Integer executionLimit,
            @RequestParam(required = false) Integer failureLimit) {
        return syncMonitoringService.getOverview(executionLimit, failureLimit);
    }
}
