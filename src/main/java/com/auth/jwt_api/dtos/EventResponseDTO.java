package com.auth.jwt_api.dtos;

import java.time.Instant;
import java.util.UUID;

import com.auth.jwt_api.models.Event;

public record EventResponseDTO(
        UUID id,
        String title,
        String description,
        Instant eventDate,
        String location,
        UUID organizerId,
        Instant createdAt,
        Instant updatedAt) {

    public static EventResponseDTO from(Event event) {
        return new EventResponseDTO(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer().getId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
