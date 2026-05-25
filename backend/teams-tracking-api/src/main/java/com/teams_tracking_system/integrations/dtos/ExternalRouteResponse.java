package com.teams_tracking_system.integrations.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalRouteResponse(
        String agentId,
        LocalDate date,
        List<ExternalRoutePointResponse> points) {
}
