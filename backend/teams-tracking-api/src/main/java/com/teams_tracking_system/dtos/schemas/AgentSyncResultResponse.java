package com.teams_tracking_system.dtos.schemas;

public record AgentSyncResultResponse(
        int read,
        int created,
        int updated,
        int skipped) {
}
