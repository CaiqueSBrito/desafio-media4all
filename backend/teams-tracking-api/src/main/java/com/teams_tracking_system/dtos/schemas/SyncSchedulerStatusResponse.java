package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.SyncExecutionStatus;
import com.teams_tracking_system.model.SyncType;
import java.time.Instant;

public record SyncSchedulerStatusResponse(
        SyncType syncType,
        SyncExecutionStatus status,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        String lastCursorValue,
        Instant lastSuccessfulSyncAt,
        int retryAttempts,
        int rateLimitErrors,
        int serviceUnavailableErrors,
        String errorMessage) {
}
