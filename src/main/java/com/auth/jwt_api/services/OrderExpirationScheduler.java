package com.auth.jwt_api.services;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Varre periodicamente reservas PENDING vencidas e as expira, devolvendo os assentos ao lote.
 * Orquestra a partir de um bean distinto do {@link OrderService} para que cada {@code expireOrder}
 * abra sua própria transação (evita o problema de auto-invocação do proxy do Spring).
 */
@Component
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

    private final OrderService orderService;

    public OrderExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.order.expiration-scan-ms:60000}")
    public void expireOverdueReservations() {
        List<UUID> overdue = orderService.findOverdueOrderIds();
        if (overdue.isEmpty()) {
            return;
        }
        log.info("Expirando {} reserva(s) vencida(s)", overdue.size());
        for (UUID id : overdue) {
            try {
                orderService.expireOrder(id);
            } catch (RuntimeException e) {
                // Não deixa um pedido problemático abortar a varredura dos demais.
                log.warn("Falha ao expirar pedido {}: {}", id, e.getMessage());
            }
        }
    }
}
