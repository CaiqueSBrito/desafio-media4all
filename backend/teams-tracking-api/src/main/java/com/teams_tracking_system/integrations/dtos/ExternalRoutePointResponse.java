package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalRoutePointResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracy,
        Instant timestamp) {
}
