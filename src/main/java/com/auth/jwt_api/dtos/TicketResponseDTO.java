package com.auth.jwt_api.dtos;

import java.util.UUID;

import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketStatus;

public record TicketResponseDTO(
        UUID id,
        UUID ticketBatchId,
        String codeHash,
        TicketStatus status) {

    public static TicketResponseDTO from(Ticket ticket) {
        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getTicketBatch().getId(),
                ticket.getCodeHash(),
                ticket.getStatus());
    }
}
