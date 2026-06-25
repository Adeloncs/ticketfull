package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String email) {
        super("User not found: " + email, HttpStatus.NOT_FOUND);
    }
}
