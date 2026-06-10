package com.auth.jwt_api.dtos;

import com.auth.jwt_api.models.UserRole;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDTO(
        @NotBlank String email,
        @NotBlank String password,
        UserRole role
) {
}
