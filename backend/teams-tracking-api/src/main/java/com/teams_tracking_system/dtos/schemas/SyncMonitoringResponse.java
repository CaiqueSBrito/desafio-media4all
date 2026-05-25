package com.teams_tracking_system.dtos.schemas;

import java.time.Instant;
import java.util.List;

public record SyncMonitoringResponse(
        Instant generatedAt,
        SyncMonitoringTotalsResponse totals,
        List<SyncSchedulerStatusResponse> schedulers,
        List<SyncExecutionMonitoringResponse> latestExecutions,
        List<SyncFailureMonitoringResponse> recentFailures,
        List<SyncCursorMonitoringResponse> cursors) {
}
