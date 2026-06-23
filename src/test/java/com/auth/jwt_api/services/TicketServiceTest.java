package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.exceptions.EventOwnershipException;
import com.auth.jwt_api.exceptions.TicketAlreadyUsedException;
import com.auth.jwt_api.exceptions.TicketNotFoundException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    private static final String CODE = "abc123";

    private User user(UUID id) {
        return User.builder().id(id).email(id + "@example.com").role(UserRole.ORGANIZER).build();
    }

    private Ticket ticketFor(User eventOwner, TicketStatus status) {
        Event event = Event.builder().id(UUID.randomUUID()).organizer(eventOwner).build();
        TicketBatch batch = TicketBatch.builder().id(UUID.randomUUID()).event(event).build();
        return Ticket.builder()
                .id(UUID.randomUUID())
                .codeHash(CODE)
                .status(status)
                .ticketBatch(batch)
                .build();
    }

    @Test
    @DisplayName("validate: marca o ingresso como USED para o organizador dono")
    void validate_shouldMarkUsed() {
        User owner = user(UUID.randomUUID());
        Ticket ticket = ticketFor(owner, TicketStatus.VALID);

        when(ticketRepository.findByCodeHashForUpdate(CODE)).thenReturn(Optional.of(ticket));

        TicketResponseDTO result = ticketService.validate(CODE, owner);

        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    @DisplayName("validate: lança TicketNotFoundException quando o código não existe")
    void validate_shouldThrow_whenMissing() {
        User owner = user(UUID.randomUUID());
        when(ticketRepository.findByCodeHashForUpdate(CODE)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> ticketService.validate(CODE, owner));
    }

    @Test
    @DisplayName("validate: lança EventOwnershipException quando o organizador não é dono do evento")
    void validate_shouldThrow_whenNotOwner() {
        User owner = user(UUID.randomUUID());
        User intruder = user(UUID.randomUUID());
        Ticket ticket = ticketFor(owner, TicketStatus.VALID);

        when(ticketRepository.findByCodeHashForUpdate(CODE)).thenReturn(Optional.of(ticket));

        assertThrows(EventOwnershipException.class, () -> ticketService.validate(CODE, intruder));
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.VALID); // não alterado
    }

    @Test
    @DisplayName("validate: lança TicketAlreadyUsedException quando o ingresso já foi usado")
    void validate_shouldThrow_whenAlreadyUsed() {
        User owner = user(UUID.randomUUID());
        Ticket ticket = ticketFor(owner, TicketStatus.USED);

        when(ticketRepository.findByCodeHashForUpdate(CODE)).thenReturn(Optional.of(ticket));

        assertThrows(TicketAlreadyUsedException.class, () -> ticketService.validate(CODE, owner));
    }
}
