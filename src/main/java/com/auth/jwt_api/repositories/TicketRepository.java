package com.auth.jwt_api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auth.jwt_api.models.Ticket;

import jakarta.persistence.LockModeType;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByCodeHash(String codeHash);

    /**
     * Carrega o ingresso com lock pessimista de escrita, evitando que dois
     * check-ins concorrentes do mesmo código marquem o ingresso como usado em duplicidade.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.codeHash = :codeHash")
    Optional<Ticket> findByCodeHashForUpdate(@Param("codeHash") String codeHash);

    /** Carrega o ingresso por id com lock pessimista (transferência). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") UUID id);
}
