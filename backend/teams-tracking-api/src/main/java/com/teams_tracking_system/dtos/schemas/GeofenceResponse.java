package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.GeofenceType;
import java.time.Instant;

public record GeofenceResponse(
        String id,
        String externalId,
        String name,
        GeofenceType type,
        String coordinatesJson,
        boolean alertOnEnter,
        boolean alertOnExit,
        String assignedTeams,
        Instant syncedAt) {
}
