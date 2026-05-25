package com.teams_tracking_system.dtos.schemas;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(
            String code,
            String message,
            String details) {
    }
}
