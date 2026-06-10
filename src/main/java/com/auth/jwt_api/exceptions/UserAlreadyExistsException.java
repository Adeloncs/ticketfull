package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException() {
        super("User already exists", HttpStatus.CONFLICT);
    }

    public UserAlreadyExistsException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
