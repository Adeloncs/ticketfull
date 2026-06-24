package com.auth.jwt_api.dtos;

import com.auth.jwt_api.models.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Criação de usuário com papel explícito, restrita a administradores. */
public record CreateUserRequestDTO(
        @NotBlank String email,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
