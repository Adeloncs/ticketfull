package com.auth.jwt_api.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.auth.jwt_api.payments.PaymentIntent;

/**
 * Dados de checkout retornados ao iniciar o pagamento de um pedido. O {@code clientSecret} é usado
 * pelo front-end para concluir o pagamento junto ao gateway; a confirmação chega depois via webhook.
 */
public record CheckoutResponseDTO(
        UUID orderId,
        String paymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency) {

    public static CheckoutResponseDTO of(UUID orderId, PaymentIntent intent) {
        return new CheckoutResponseDTO(orderId, intent.id(), intent.clientSecret(), intent.amount(), intent.currency());
    }
}
