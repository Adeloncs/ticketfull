package com.auth.jwt_api.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;

import com.auth.jwt_api.dtos.EventRequestDTO;
import com.auth.jwt_api.dtos.EventResponseDTO;
import com.auth.jwt_api.exceptions.EventNotFoundException;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.exceptions.InvalidEventStateException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.EventStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.EventRepository;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponseDTO create(EventRequestDTO request, User organizer) {
        Event event = Event.builder()
                .title(request.title())
                .description(request.description())
                .eventDate(request.eventDate())
                .location(request.location())
                .organizer(organizer)
                .build();

        return EventResponseDTO.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<EventResponseDTO> search(String location, Instant from, Instant to, Pageable pageable) {
        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Busca pública: apenas eventos publicados
            predicates.add(cb.equal(root.get("status"), EventStatus.PUBLISHED));
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return eventRepository.findAll(spec, pageable).map(EventResponseDTO::from);
    }

    /** Atualiza os dados de um evento do próprio organizador. Não permitido em eventos cancelados. */
    @Transactional
    public EventResponseDTO update(UUID id, EventRequestDTO request, User organizer) {
        Event event = getEntity(id);
        requireOwnership(event, organizer);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventStateException(id, event.getStatus(), "updated");
        }
        event.updateDetails(request.title(), request.description(), request.eventDate(), request.location());
        return EventResponseDTO.from(event);
    }

    /** Publica um evento DRAFT do próprio organizador, tornando-o visível e disponível para venda. */
    @Transactional
    public EventResponseDTO publish(UUID id, User organizer) {
        Event event = getEntity(id);
        requireOwnership(event, organizer);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new InvalidEventStateException(id, event.getStatus(), "published");
        }
        event.markAsPublished();
        return EventResponseDTO.from(event);
    }

    /** Cancela um evento do próprio organizador (encerra novas vendas). */
    @Transactional
    public EventResponseDTO cancel(UUID id, User organizer) {
        Event event = getEntity(id);
        requireOwnership(event, organizer);
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new InvalidEventStateException(id, event.getStatus(), "cancelled");
        }
        event.markAsCancelled();
        return EventResponseDTO.from(event);
    }

    private void requireOwnership(Event event, User organizer) {
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new EventOwnershipException();
        }
    }

    @Transactional(readOnly = true)
    public EventResponseDTO findById(UUID id) {
        return EventResponseDTO.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Event getEntity(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }
}
