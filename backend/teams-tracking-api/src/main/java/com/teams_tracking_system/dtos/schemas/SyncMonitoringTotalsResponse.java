package com.teams_tracking_system.dtos.schemas;

public record SyncMonitoringTotalsResponse(
        long executions,
        long failures,
        long recordsRead,
        long recordsCreated,
        long recordsUpdated,
        long recordsIgnored,
        long recordsFailed,
        long retryAttempts,
        long rateLimitErrors,
        long serviceUnavailableErrors) {
}
