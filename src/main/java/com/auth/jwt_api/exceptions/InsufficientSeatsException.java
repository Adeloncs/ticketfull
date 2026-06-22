package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class InsufficientSeatsException extends BusinessException {

    public InsufficientSeatsException(int available, int requested) {
        super("Insufficient seats: requested " + requested + ", available " + available,
                HttpStatus.CONFLICT);
    }
}
