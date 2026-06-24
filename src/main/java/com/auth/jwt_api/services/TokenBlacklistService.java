package com.auth.jwt_api.services;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.models.RevokedToken;
import com.auth.jwt_api.repositories.RevokedTokenRepository;

/**
 * Lista de bloqueio de tokens de acesso. Permite invalidar um JWT antes da sua expiração natural
 * (ex.: no logout), consultada pelo filtro de segurança a cada requisição autenticada.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenBlacklistService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /** Revoga um token pelo seu jti até o instante de expiração informado. Idempotente. */
    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || expiresAt == null) {
            return;
        }
        revokedTokenRepository.save(RevokedToken.builder().jti(jti).expiresAt(expiresAt).build());
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    /** Remove periodicamente entradas de tokens já expirados (não há motivo para mantê-las). */
    @Scheduled(fixedDelayString = "${app.security.revoked-token-purge-ms:3600000}")
    @Transactional
    public void purgeExpired() {
        int removed = revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Removidas {} entrada(s) de tokens revogados expirados", removed);
        }
    }
}
