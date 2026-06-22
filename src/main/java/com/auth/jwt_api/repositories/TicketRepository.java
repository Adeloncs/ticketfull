package com.auth.jwt_api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth.jwt_api.models.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByCodeHash(String codeHash);
}
