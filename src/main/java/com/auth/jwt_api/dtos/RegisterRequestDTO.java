package com.auth.jwt_api.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Auto-registro público. O papel não é aceito do cliente: todo registro cria um CUSTOMER.
 * Papéis privilegiados (ORGANIZER/ADMIN) são provisionados por um ADMIN via {@code POST /admin/users}.
 */
public record RegisterRequestDTO(
        @NotBlank String email,
        @NotBlank String password
) {
}
