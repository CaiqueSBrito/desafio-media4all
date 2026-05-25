package com.teams_tracking_system.dtos.requests;

import com.teams_tracking_system.model.CheckInType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record ManualCheckInRequest(
        @NotNull(message = "type is required")
        CheckInType type,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
        BigDecimal latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
        BigDecimal longitude,

        @Size(max = 500, message = "address must have at most 500 characters")
        String address,

        @DecimalMin(value = "0.0", message = "accuracy must be greater than or equal to 0")
        BigDecimal accuracy,

        @DecimalMin(value = "0.0", message = "speed must be greater than or equal to 0")
        BigDecimal speed,

        @Size(max = 1000, message = "notes must have at most 1000 characters")
        String notes,

        Instant occurredAt,

        @Size(max = 100, message = "idempotencyKey must have at most 100 characters")
        String idempotencyKey) {
}
