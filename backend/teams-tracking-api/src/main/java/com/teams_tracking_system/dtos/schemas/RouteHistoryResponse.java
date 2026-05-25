package com.teams_tracking_system.dtos.schemas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RouteHistoryResponse(
        String agentId,
        String agentName,
        LocalDate date,
        BigDecimal totalDistanceMeters,
        List<RoutePointResponse> points) {
}
