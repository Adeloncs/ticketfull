package com.auth.jwt_api.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.OrderStatus;

public record OrderResponseDTO(
        UUID id,
        UUID customerId,
        UUID eventId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<TicketResponseDTO> tickets,
        Instant createdAt) {

    public static OrderResponseDTO from(Order order) {
        return new OrderResponseDTO(
                order.getId(),
                order.getCustomer().getId(),
                order.getEvent().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getTickets().stream().map(TicketResponseDTO::from).toList(),
                order.getCreatedAt());
    }
}
