package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth.jwt_api.dtos.TicketBatchRequestDTO;
import com.auth.jwt_api.dtos.TicketBatchResponseDTO;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.TicketBatchRepository;

@ExtendWith(MockitoExtension.class)
class TicketBatchServiceTest {

    @Mock
    private TicketBatchRepository ticketBatchRepository;

    @Mock
    private EventService eventService;

    @InjectMocks
    private TicketBatchService ticketBatchService;

    private User user(UUID id) {
        return User.builder().id(id).email(id + "@example.com").role(UserRole.ORGANIZER).build();
    }

    private Event eventOwnedBy(User owner) {
        return Event.builder().id(UUID.randomUUID()).title("E").location("L").organizer(owner).build();
    }

    @Test
    @DisplayName("addToEvent: inicia availableSeats igual a totalCapacity para o dono do evento")
    void addToEvent_shouldInitializeAvailableSeats() {
        User organizer = user(UUID.randomUUID());
        Event event = eventOwnedBy(organizer);
        TicketBatchRequestDTO request = new TicketBatchRequestDTO("Pista", new BigDecimal("100.00"), 500, null, null);

        when(eventService.getEntity(event.getId())).thenReturn(event);
        when(ticketBatchRepository.save(any(TicketBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        TicketBatchResponseDTO result = ticketBatchService.addToEvent(event.getId(), request, organizer);

        assertThat(result.totalCapacity()).isEqualTo(500);
        assertThat(result.availableSeats()).isEqualTo(500);
        assertThat(result.price()).isEqualByComparingTo("100.00");
        assertThat(result.eventId()).isEqualTo(event.getId());
    }

    @Test
    @DisplayName("addToEvent: lança EventOwnershipException quando quem chama não é o dono")
    void addToEvent_shouldThrow_whenNotOwner() {
        User owner = user(UUID.randomUUID());
        User intruder = user(UUID.randomUUID());
        Event event = eventOwnedBy(owner);
        TicketBatchRequestDTO request = new TicketBatchRequestDTO("Pista", new BigDecimal("100.00"), 500, null, null);

        when(eventService.getEntity(event.getId())).thenReturn(event);

        assertThrows(EventOwnershipException.class,
                () -> ticketBatchService.addToEvent(event.getId(), request, intruder));
        verify(ticketBatchRepository, never()).save(any());
    }
}
