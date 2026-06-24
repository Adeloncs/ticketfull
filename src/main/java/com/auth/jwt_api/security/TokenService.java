package com.auth.jwt_api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth.jwt_api.models.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration}")
    private long expiration;

    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString()) // jti: identifica o token para revogação
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /** Retorna o subject (email) do token, ou string vazia se inválido/expirado. */
    public String validateToken(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? "" : claims.getSubject();
    }

    /** Identificador único do token (jti), ou {@code null} se inválido. */
    public String extractTokenId(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getId();
    }

    /** Instante de expiração do token, ou {@code null} se inválido. */
    public Instant extractExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getExpiration().toInstant();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
