package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.auth.jwt_api.models.OrderStatus;

public class OrderNotPayableException extends BusinessException {

    public OrderNotPayableException(UUID id, OrderStatus currentStatus) {
        super("Order " + id + " cannot be paid because its status is " + currentStatus,
                HttpStatus.CONFLICT);
    }

    public OrderNotPayableException(UUID id) {
        super("Order " + id + " cannot be paid because its reservation has expired",
                HttpStatus.CONFLICT);
    }
}
