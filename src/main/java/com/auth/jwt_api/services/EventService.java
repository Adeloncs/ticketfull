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
import com.auth.jwt_api.models.Event;
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
