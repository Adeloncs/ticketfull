package com.auth.jwt_api.dtos;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de criação/atualização de evento. O organizador NÃO vem no corpo —
 * é resolvido a partir do usuário autenticado.
 */
public record EventRequestDTO(
        @NotBlank String title,
        String description,
        @NotNull @Future Instant eventDate,
        @NotBlank String location) {
}
