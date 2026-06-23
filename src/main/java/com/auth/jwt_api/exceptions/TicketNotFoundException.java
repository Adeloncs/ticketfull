package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class TicketNotFoundException extends BusinessException {

    public TicketNotFoundException(String codeHash) {
        super("Ticket not found for code: " + codeHash, HttpStatus.NOT_FOUND);
    }
}
