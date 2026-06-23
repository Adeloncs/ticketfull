package com.auth.jwt_api.models;

public enum OrderStatus {
    /** Reserva criada, assentos retidos, aguardando pagamento (com prazo de expiração). */
    PENDING,
    /** Pagamento confirmado pelo gateway via webhook. */
    PAID,
    /** Cancelado pelo cliente antes do pagamento; assentos devolvidos. */
    CANCELLED,
    /** Reserva expirou sem pagamento; assentos devolvidos automaticamente. */
    EXPIRED,
    /** Pedido pago e posteriormente estornado; assentos devolvidos. */
    REFUNDED
}
