package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.auth.jwt_api.models.OrderStatus;

public class OrderNotCancellableException extends BusinessException {

    public OrderNotCancellableException(UUID id, OrderStatus currentStatus) {
        super("Order " + id + " cannot be cancelled because its status is " + currentStatus,
                HttpStatus.CONFLICT);
    }
}
