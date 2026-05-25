package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.SyncSource;
import java.math.BigDecimal;
import java.time.Instant;

public record RoutePointResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal accuracy,
        BigDecimal speed,
        String address,
        Instant timestamp,
        SyncSource source) {
}
