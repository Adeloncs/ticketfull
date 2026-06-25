package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.auth.jwt_api.models.EventStatus;

public class InvalidEventStateException extends BusinessException {

    public InvalidEventStateException(UUID id, EventStatus current, String action) {
        super("Event " + id + " cannot be " + action + " because its status is " + current, HttpStatus.CONFLICT);
    }
}
