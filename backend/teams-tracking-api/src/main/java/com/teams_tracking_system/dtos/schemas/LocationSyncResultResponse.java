package com.teams_tracking_system.dtos.schemas;

public record LocationSyncResultResponse(
        int read,
        int positionsCreated,
        int agentsUpdated,
        int ignored,
        int skipped) {
}
