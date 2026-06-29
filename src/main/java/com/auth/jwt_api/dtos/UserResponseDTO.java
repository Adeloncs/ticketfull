package com.auth.jwt_api.dtos;

import java.time.Instant;
import java.util.UUID;

import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;

/** Perfil do usuário autenticado (retornado por GET /me). */
public record UserResponseDTO(
        UUID id,
        String email,
        UserRole role,
        Instant createdAt) {

    public static UserResponseDTO from(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt());
    }
}
