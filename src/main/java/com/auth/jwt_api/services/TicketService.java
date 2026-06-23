package com.auth.jwt_api.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.exceptions.TicketAlreadyUsedException;
import com.auth.jwt_api.exceptions.TicketNotFoundException;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.TicketRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Check-in na portaria: valida o ingresso pelo código e o marca como USED.
     * Só o organizador dono do evento do ingresso pode validar.
     */
    @Transactional
    public TicketResponseDTO validate(String codeHash, User organizer) {
        Ticket ticket = ticketRepository.findByCodeHashForUpdate(codeHash)
                .orElseThrow(() -> new TicketNotFoundException(codeHash));

        UUID eventOrganizerId = ticket.getTicketBatch().getEvent().getOrganizer().getId();
        if (!eventOrganizerId.equals(organizer.getId())) {
            throw new EventOwnershipException();
        }

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new TicketAlreadyUsedException(codeHash);
        }

        ticket.markAsUsed();
        return TicketResponseDTO.from(ticket);
    }
}
