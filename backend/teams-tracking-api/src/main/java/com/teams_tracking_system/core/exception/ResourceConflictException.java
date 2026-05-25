package com.teams_tracking_system.core.exception;

import org.springframework.http.HttpStatus;

public class ResourceConflictException extends ApiException {

    public ResourceConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }

    public ResourceConflictException(String message, String details) {
        super(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message, details);
    }
}
