package com.auth.jwt_api.exceptions;

import org.springframework.http.HttpStatus;

public class PaymentIntentNotFoundException extends BusinessException {

    public PaymentIntentNotFoundException(String paymentIntentId) {
        super("No order found for payment intent " + paymentIntentId, HttpStatus.NOT_FOUND);
    }
}
