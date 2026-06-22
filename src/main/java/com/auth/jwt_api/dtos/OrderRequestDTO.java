package com.auth.jwt_api.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Pedido de compra: quantidade de ingressos de um lote específico. O cliente vem
 * do usuário autenticado e o evento é derivado do lote.
 */
public record OrderRequestDTO(
        @NotNull UUID ticketBatchId,
        @NotNull @Positive Integer quantity) {
}
