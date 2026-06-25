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
import com.auth.jwt_api.exceptions.TicketTransferForbiddenException;
import com.auth.jwt_api.exceptions.UserNotFoundException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.TicketRepository;
import com.auth.jwt_api.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

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

    private Ticket transferableTicket(User owner, TicketStatus status) {
        Order order = Order.builder().id(UUID.randomUUID()).customer(owner).build();
        Event event = Event.builder().id(UUID.randomUUID()).organizer(owner).build();
        TicketBatch batch = TicketBatch.builder().id(UUID.randomUUID()).event(event).build();
        return Ticket.builder()
                .id(UUID.randomUUID())
                .order(order)
                .ticketBatch(batch)
                .codeHash("oldhash")
                .status(status)
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

    @Test
    @DisplayName("transfer: reatribui o detentor e regenera o código (QR) para o novo dono")
    void transfer_shouldReassignHolderAndRegenerateCode() {
        User owner = user(UUID.randomUUID());
        User target = user(UUID.randomUUID());
        Ticket ticket = transferableTicket(owner, TicketStatus.VALID);

        when(ticketRepository.findByIdForUpdate(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail(target.getEmail())).thenReturn(Optional.of(target));

        TicketResponseDTO result = ticketService.transfer(ticket.getId(), target.getEmail(), owner);

        assertThat(ticket.getHolder()).isEqualTo(target);
        assertThat(ticket.getCodeHash()).isNotEqualTo("oldhash");
        assertThat(result.holderId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("transfer: lança TicketTransferForbiddenException quando o requester não é o detentor")
    void transfer_shouldThrow_whenNotOwner() {
        User owner = user(UUID.randomUUID());
        User intruder = user(UUID.randomUUID());
        Ticket ticket = transferableTicket(owner, TicketStatus.VALID);

        when(ticketRepository.findByIdForUpdate(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(TicketTransferForbiddenException.class,
                () -> ticketService.transfer(ticket.getId(), "x@example.com", intruder));
    }

    @Test
    @DisplayName("transfer: lança TicketAlreadyUsedException quando o ingresso já foi usado")
    void transfer_shouldThrow_whenUsed() {
        User owner = user(UUID.randomUUID());
        Ticket ticket = transferableTicket(owner, TicketStatus.USED);

        when(ticketRepository.findByIdForUpdate(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThrows(TicketAlreadyUsedException.class,
                () -> ticketService.transfer(ticket.getId(), "x@example.com", owner));
    }

    @Test
    @DisplayName("transfer: lança UserNotFoundException quando o destinatário não existe")
    void transfer_shouldThrow_whenTargetMissing() {
        User owner = user(UUID.randomUUID());
        Ticket ticket = transferableTicket(owner, TicketStatus.VALID);

        when(ticketRepository.findByIdForUpdate(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> ticketService.transfer(ticket.getId(), "ghost@example.com", owner));
    }
}
