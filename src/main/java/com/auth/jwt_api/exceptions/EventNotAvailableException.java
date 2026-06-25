package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class EventNotAvailableException extends BusinessException {

    public EventNotAvailableException(UUID eventId) {
        super("Event " + eventId + " is not available for purchase (not published)", HttpStatus.CONFLICT);
    }
}
