package com.teams_tracking_system.dtos.schemas;

import com.teams_tracking_system.model.CheckInType;
import com.teams_tracking_system.model.SyncSource;
import java.math.BigDecimal;
import java.time.Instant;

public record CheckInResponse(
        String id,
        String agentId,
        CheckInType type,
        SyncSource source,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        BigDecimal accuracy,
        BigDecimal speed,
        String notes,
        BigDecimal distanceFromPrevious,
        String externalEventId,
        String manualIdempotencyKey,
        Instant occurredAt,
        Instant syncedAt) {

    public static CheckInResponse fromEntity(com.teams_tracking_system.model.CheckIn checkIn) {
        return new CheckInResponse(
                checkIn.getId(),
                checkIn.getAgent().getId(),
                checkIn.getType(),
                checkIn.getSource(),
                checkIn.getLatitude(),
                checkIn.getLongitude(),
                checkIn.getAddress(),
                checkIn.getAccuracy(),
                checkIn.getSpeed(),
                checkIn.getNotes(),
                checkIn.getDistanceFromPrevious(),
                checkIn.getExternalEventId(),
                checkIn.getManualIdempotencyKey(),
                checkIn.getOccurredAt(),
                checkIn.getSyncedAt());
    }
}
