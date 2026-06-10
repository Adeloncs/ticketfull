package com.auth.jwt_api.dtos;

public record LoginResponseDTO(String accessToken, String tokenType, long expiresIn) {
}
