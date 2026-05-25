package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalAgentResponse(
        String id,
        String externalId,
        String name,
        ExternalAgentRole role,
        String team,
        String phone,
        String email,
        Boolean active,
        ExternalAgentStatus status,
        Integer battery,
        Instant lastSeen,
        Instant createdAt,
        Instant updatedAt) {
}
