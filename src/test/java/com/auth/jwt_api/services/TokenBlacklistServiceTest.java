package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth.jwt_api.models.RevokedToken;
import com.auth.jwt_api.repositories.RevokedTokenRepository;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenBlacklistService service;

    @Test
    @DisplayName("revoke: persiste o jti com a expiração informada")
    void revoke_shouldPersist() {
        Instant exp = Instant.now().plusSeconds(3600);

        service.revoke("jti-1", exp);

        verify(revokedTokenRepository).save(any(RevokedToken.class));
    }

    @Test
    @DisplayName("revoke: ignora jti/expiração nulos")
    void revoke_shouldIgnoreNulls() {
        service.revoke(null, Instant.now());
        service.revoke("jti", null);

        verify(revokedTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("isRevoked: delega ao repositório; nulo nunca está revogado")
    void isRevoked_shouldDelegate() {
        when(revokedTokenRepository.existsById("jti-1")).thenReturn(true);

        assertThat(service.isRevoked("jti-1")).isTrue();
        assertThat(service.isRevoked(null)).isFalse();
    }

    @Test
    @DisplayName("purgeExpired: remove entradas já expiradas")
    void purgeExpired_shouldDeleteExpired() {
        when(revokedTokenRepository.deleteByExpiresAtBefore(any())).thenReturn(2);

        service.purgeExpired();

        verify(revokedTokenRepository).deleteByExpiresAtBefore(any());
    }
}
