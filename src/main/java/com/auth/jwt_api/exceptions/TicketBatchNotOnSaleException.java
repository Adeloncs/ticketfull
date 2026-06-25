package com.auth.jwt_api.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class TicketBatchNotOnSaleException extends BusinessException {

    public TicketBatchNotOnSaleException(UUID batchId) {
        super("Ticket batch " + batchId + " is outside its sales window", HttpStatus.CONFLICT);
    }
}
