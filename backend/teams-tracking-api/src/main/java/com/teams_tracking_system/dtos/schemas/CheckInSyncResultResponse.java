package com.teams_tracking_system.dtos.schemas;

public record CheckInSyncResultResponse(
        int synced,
        String previousSyncToken,
        String syncToken) {
}
