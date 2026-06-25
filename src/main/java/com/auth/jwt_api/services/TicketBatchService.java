package com.auth.jwt_api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.TicketBatchRequestDTO;
import com.auth.jwt_api.dtos.TicketBatchResponseDTO;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.TicketBatchRepository;

@Service
public class TicketBatchService {

    private final TicketBatchRepository ticketBatchRepository;
    private final EventService eventService;

    public TicketBatchService(TicketBatchRepository ticketBatchRepository, EventService eventService) {
        this.ticketBatchRepository = ticketBatchRepository;
        this.eventService = eventService;
    }

    @Transactional
    public TicketBatchResponseDTO addToEvent(UUID eventId, TicketBatchRequestDTO request, User organizer) {
        Event event = eventService.getEntity(eventId);
        requireOwnership(event, organizer);

        TicketBatch batch = TicketBatch.builder()
                .event(event)
                .name(request.name())
                .price(request.price())
                .totalCapacity(request.totalCapacity())
                .availableSeats(request.totalCapacity()) // lote inicia com toda a capacidade disponível
                .salesStartAt(request.salesStartAt())
                .salesEndAt(request.salesEndAt())
                .build();

        return TicketBatchResponseDTO.from(ticketBatchRepository.save(batch));
    }

    @Transactional(readOnly = true)
    public List<TicketBatchResponseDTO> listByEvent(UUID eventId) {
        eventService.getEntity(eventId); // garante 404 se o evento não existir
        return ticketBatchRepository.findByEventId(eventId).stream()
                .map(TicketBatchResponseDTO::from)
                .toList();
    }

    private void requireOwnership(Event event, User organizer) {
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new EventOwnershipException();
        }
    }
}
