package com.auth.jwt_api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<EventResponseDTO> findAll() {
        return eventRepository.findAll().stream()
                .map(EventResponseDTO::from)
                .toList();
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
