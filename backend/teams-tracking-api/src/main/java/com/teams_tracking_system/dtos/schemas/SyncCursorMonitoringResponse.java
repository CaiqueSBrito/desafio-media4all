package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.SyncType;
import java.time.Instant;

public record SyncCursorMonitoringResponse(
        SyncType syncType,
        String lastCursorValue,
        Integer lastPage,
        Instant lastOccurredAt,
        Instant lastSyncedAt,
        Instant lastSuccessfulSyncAt,
        Instant updatedAt) {
}
