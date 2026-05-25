package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import java.time.Instant;

public record SyncExecutionMonitoringResponse(
        Long id,
        SyncType syncType,
        SyncExecutionStatus status,
        Instant startedAt,
        Instant finishedAt,
        Long durationMillis,
        int recordsRead,
        int recordsCreated,
        int recordsUpdated,
        int recordsIgnored,
        int recordsFailed,
        int retryAttempts,
        int rateLimitErrors,
        int serviceUnavailableErrors,
        String cursorValueBefore,
        String cursorValueAfter,
        String errorMessage) {
}
