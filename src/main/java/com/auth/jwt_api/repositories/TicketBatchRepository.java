package com.auth.jwt_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.jwt_api.models.TicketBatch;

public interface TicketBatchRepository extends JpaRepository<TicketBatch, UUID> {

    List<TicketBatch> findByEventId(UUID eventId);
}
