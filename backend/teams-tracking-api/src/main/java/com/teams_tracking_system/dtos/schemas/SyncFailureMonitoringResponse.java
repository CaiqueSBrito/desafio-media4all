package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.SyncType;
import java.time.Instant;

public record SyncFailureMonitoringResponse(
        Long id,
        Long syncExecutionId,
        SyncType syncType,
        String entityType,
        String reason,
        Instant createdAt) {
}
