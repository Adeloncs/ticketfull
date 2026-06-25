package com.auth.jwt_api.events;

import java.util.UUID;

/**
 * Evento de domínio publicado quando um pedido é confirmado como pago. Consumido de forma assíncrona
 * (após o commit) para disparar notificações ao cliente, desacoplando-as da transação de pagamento.
 */
public record OrderPaidEvent(UUID orderId, String customerEmail, int ticketCount) {
}
