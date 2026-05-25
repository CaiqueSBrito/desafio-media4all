package com.teams_tracking_system.core.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ApiException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message);
    }

    public InvalidRequestException(String message, String details) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, details);
    }
}
