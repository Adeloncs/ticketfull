package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(UUID id) {
        super("Order not found: " + id, HttpStatus.NOT_FOUND);
    }
}
