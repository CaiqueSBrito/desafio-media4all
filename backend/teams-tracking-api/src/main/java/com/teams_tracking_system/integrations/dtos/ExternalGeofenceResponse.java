package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalGeofenceResponse(
        String id,
        String externalId,
        String name,
        ExternalGeofenceType type,
        String coordinatesJson,
        Boolean alertOnEnter,
        Boolean alertOnExit,
        String assignedTeams,
        Instant syncedAt) {
}
