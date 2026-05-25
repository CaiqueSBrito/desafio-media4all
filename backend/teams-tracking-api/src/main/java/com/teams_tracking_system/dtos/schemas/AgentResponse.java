package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.Agent;
import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AgentResponse(
        String id,
        String externalId,
        String name,
        AgentRole role,
        String team,
        String phone,
        String email,
        boolean active,
        AgentStatus status,
        Integer battery,
        Instant lastSeen,
        BigDecimal currentLatitude,
        BigDecimal currentLongitude,
        String currentAddress,
        BigDecimal currentAccuracy,
        BigDecimal currentSpeed,
        Instant createdAt,
        Instant updatedAt) {

    public static AgentResponse fromEntity(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getExternalId(),
                agent.getName(),
                agent.getRole(),
                agent.getTeam(),
                agent.getPhone(),
                agent.getEmail(),
                agent.isActive(),
                agent.getStatus(),
                agent.getBattery(),
                agent.getLastSeen(),
                agent.getCurrentLatitude(),
                agent.getCurrentLongitude(),
                agent.getCurrentAddress(),
                agent.getCurrentAccuracy(),
                agent.getCurrentSpeed(),
                agent.getCreatedAt(),
                agent.getUpdatedAt());
    }
}
