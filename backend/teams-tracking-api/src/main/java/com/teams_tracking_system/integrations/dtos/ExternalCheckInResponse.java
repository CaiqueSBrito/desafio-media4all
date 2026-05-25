package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalCheckInResponse(
        String id,
        String agentId,
        ExternalCheckInType type,
        ExternalSyncSource source,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        BigDecimal accuracy,
        BigDecimal speed,
        String notes,
        BigDecimal distanceFromPrevious,
        String externalEventId,
        Instant occurredAt,
        Instant syncedAt) {
}
