package com.auth.jwt_api.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload de criação de lote de ingressos. O evento vem do path (não do corpo) e
 * {@code availableSeats} é inicializado pelo service com o valor de {@code totalCapacity}.
 * A janela de vendas ({@code salesStartAt}/{@code salesEndAt}) é opcional — nula significa sem restrição.
 */
public record TicketBatchRequestDTO(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer totalCapacity,
        Instant salesStartAt,
        Instant salesEndAt) {
}
