package com.auth.jwt_api.dtos;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequestDTO(
        @NotBlank String email,
        @NotBlank String password
) {
}
