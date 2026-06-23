package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class TicketAlreadyUsedException extends BusinessException {

    public TicketAlreadyUsedException(String codeHash) {
        super("Ticket already used: " + codeHash, HttpStatus.CONFLICT);
    }
}
