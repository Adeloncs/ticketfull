package com.auth.jwt_api.repositories;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auth.jwt_api.models.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    /** Remove entradas cujo token já expirou naturalmente (limpeza periódica). */
    @Modifying
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
