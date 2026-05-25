package com.teams_tracking_system.dtos.schemas;

public record GeofenceSyncResultResponse(
        int read,
        int created,
        int updated,
        int skipped) {
}
