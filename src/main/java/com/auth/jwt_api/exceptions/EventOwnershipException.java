package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class EventOwnershipException extends BusinessException {

    public EventOwnershipException() {
        super("You are not the organizer of this event", HttpStatus.FORBIDDEN);
    }
}
