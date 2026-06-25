package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class TicketTransferForbiddenException extends BusinessException {

    public TicketTransferForbiddenException() {
        super("Only the current holder can transfer this ticket", HttpStatus.FORBIDDEN);
    }
}
