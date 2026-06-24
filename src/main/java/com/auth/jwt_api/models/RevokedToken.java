package com.auth.jwt_api.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Token de acesso revogado (lista de bloqueio). A chave é o {@code jti} do JWT; {@code expiresAt}
 * permite descartar entradas após a expiração natural do token, mantendo a tabela enxuta.
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    @Column(name = "jti")
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
