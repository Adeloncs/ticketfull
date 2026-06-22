package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class EventNotFoundException extends BusinessException {

    public EventNotFoundException(UUID id) {
        super("Event not found: " + id, HttpStatus.NOT_FOUND);
    }
}
