package com.auth.jwt_api.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.exceptions.TicketAlreadyUsedException;
import com.auth.jwt_api.exceptions.TicketNotFoundException;
import com.auth.jwt_api.exceptions.TicketTransferForbiddenException;
import com.auth.jwt_api.exceptions.UserNotFoundException;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.TicketRepository;
import com.auth.jwt_api.repositories.UserRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
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

    /**
     * Transfere um ingresso VÁLIDO para outro usuário (por e-mail). Só o detentor atual pode transferir;
     * o código (QR) é regenerado, invalidando o anterior. Lock pessimista evita transferências concorrentes.
     */
    @Transactional
    public TicketResponseDTO transfer(UUID ticketId, String targetEmail, User requester) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId.toString()));

        User owner = ticket.effectiveOwner();
        if (owner == null || !owner.getId().equals(requester.getId())) {
            throw new TicketTransferForbiddenException();
        }
        if (ticket.getStatus() == TicketStatus.USED) {
            throw new TicketAlreadyUsedException(ticket.getCodeHash());
        }

        User target = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new UserNotFoundException(targetEmail));

        ticket.transferTo(target, CodeHashGenerator.generate());
        return TicketResponseDTO.from(ticket);
    }
}
