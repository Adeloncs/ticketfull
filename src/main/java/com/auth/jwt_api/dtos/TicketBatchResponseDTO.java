package com.auth.jwt_api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.auth.jwt_api.models.TicketBatch;

public record TicketBatchResponseDTO(
        UUID id,
        UUID eventId,
        String name,
        BigDecimal price,
        Integer totalCapacity,
        Integer availableSeats,
        Instant salesStartAt,
        Instant salesEndAt) {

    public static TicketBatchResponseDTO from(TicketBatch batch) {
        return new TicketBatchResponseDTO(
                batch.getId(),
                batch.getEvent().getId(),
                batch.getName(),
                batch.getPrice(),
                batch.getTotalCapacity(),
                batch.getAvailableSeats(),
                batch.getSalesStartAt(),
                batch.getSalesEndAt());
    }
}
