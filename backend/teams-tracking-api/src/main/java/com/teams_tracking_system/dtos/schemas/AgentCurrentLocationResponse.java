package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.AgentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AgentCurrentLocationResponse(
        String agentId,
        String externalId,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String currentAddress,
        BigDecimal accuracy,
        BigDecimal speed,
        Integer battery,
        AgentStatus status,
        Instant lastSeen) {
}
