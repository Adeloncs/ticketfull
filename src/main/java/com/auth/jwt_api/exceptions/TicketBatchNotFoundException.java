package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class TicketBatchNotFoundException extends BusinessException {

    public TicketBatchNotFoundException(UUID id) {
        super("Ticket batch not found: " + id, HttpStatus.NOT_FOUND);
    }
}
