package com.teams_tracking_system.integrations.dtos;

public record ExternalCheckInSyncResponse(
        Integer synced,
        String syncToken) {
}
