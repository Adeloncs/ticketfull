package com.auth.jwt_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auth.jwt_api.models.TicketBatch;

import jakarta.persistence.LockModeType;

public interface TicketBatchRepository extends JpaRepository<TicketBatch, UUID> {

    List<TicketBatch> findByEventId(UUID eventId);

    /**
     * Carrega o lote com lock pessimista de escrita (SELECT ... FOR UPDATE),
     * serializando compradores concorrentes do mesmo lote e evitando overselling.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tb FROM TicketBatch tb WHERE tb.id = :id")
    Optional<TicketBatch> findByIdForUpdate(@Param("id") UUID id);
}
