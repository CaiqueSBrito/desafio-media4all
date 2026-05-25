package com.teams_tracking_system.dtos.requests;

import com.teams_tracking_system.model.AgentRole;
import com.teams_tracking_system.model.AgentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AgentCreateRequest(
        @NotBlank(message = "externalId is required")
        @Size(max = 100, message = "externalId must have at most 100 characters")
        String externalId,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must have at most 150 characters")
        String name,

        @NotNull(message = "role is required")
        AgentRole role,

        @Size(max = 100, message = "team must have at most 100 characters")
        String team,

        @Size(max = 30, message = "phone must have at most 30 characters")
        String phone,

        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must have at most 150 characters")
        String email,

        @NotNull(message = "active is required")
        Boolean active,

        @NotNull(message = "status is required")
        AgentStatus status,

        @Min(value = 0, message = "battery must be greater than or equal to 0")
        @Max(value = 100, message = "battery must be less than or equal to 100")
        Integer battery,

        @PastOrPresent(message = "lastSeen must be in the past or present")
        Instant lastSeen) {
}
