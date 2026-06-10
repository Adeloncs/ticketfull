package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super("Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
    }
}
