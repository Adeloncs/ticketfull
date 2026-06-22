package com.auth.jwt_api.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload de criação de lote de ingressos. O evento vem do path (não do corpo) e
 * {@code availableSeats} é inicializado pelo service com o valor de {@code totalCapacity}.
 */
public record TicketBatchRequestDTO(
        @NotBlank String name,
        @NotNull @Positive BigDecimal price,
        @NotNull @Positive Integer totalCapacity) {
}
