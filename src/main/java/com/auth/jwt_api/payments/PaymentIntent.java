package com.auth.jwt_api.payments;

import java.math.BigDecimal;

/**
 * Resultado da criação de uma intenção de pagamento no gateway.
 *
 * @param id           identificador do PaymentIntent no provedor (ex.: "pi_...")
 * @param clientSecret segredo usado pelo front-end para concluir o pagamento
 * @param amount       valor a ser cobrado
 * @param currency     moeda (ex.: "BRL")
 */
public record PaymentIntent(String id, String clientSecret, BigDecimal amount, String currency) {
}
