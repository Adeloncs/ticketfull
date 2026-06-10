package com.auth.jwt_api.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.exceptions.InvalidRefreshTokenException;
import com.auth.jwt_api.models.RefreshToken;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.RefreshTokenRepository;
import com.auth.jwt_api.security.TokenService;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;

    @Value("${api.security.token.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TokenService tokenService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshExpiration))
                .user(user)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public AuthService.LoginResult rotateRefreshToken(String token, long expiresInSeconds) {
        RefreshToken existing = refreshTokenRepository.findByToken(token)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(existing);
            throw new InvalidRefreshTokenException();
        }

        User user = existing.getUser();

        refreshTokenRepository.delete(existing);

        RefreshToken newRefreshToken = createRefreshToken(user);
        String newAccessToken = tokenService.generateToken(user);

        return new AuthService.LoginResult(newAccessToken, newRefreshToken.getToken(), expiresInSeconds);
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }
}
