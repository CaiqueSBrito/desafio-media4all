package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalLocationResponse(
        String agentId,
        String externalId,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        String currentAddress,
        BigDecimal accuracy,
        BigDecimal speed,
        Integer battery,
        ExternalAgentStatus status,
        Instant lastSeen) {
}
