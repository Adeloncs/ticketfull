package com.auth.jwt_api.dtos;

import java.util.UUID;

import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;

public record TicketResponseDTO(
        UUID id,
        UUID ticketBatchId,
        String codeHash,
        TicketStatus status,
        UUID holderId) {

    public static TicketResponseDTO from(Ticket ticket) {
        User owner = ticket.effectiveOwner();
        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getTicketBatch().getId(),
                ticket.getCodeHash(),
                ticket.getStatus(),
                owner != null ? owner.getId() : null);
    }
}
