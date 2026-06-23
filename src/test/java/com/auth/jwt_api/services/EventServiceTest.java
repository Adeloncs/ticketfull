package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.auth.jwt_api.dtos.EventRequestDTO;
import com.auth.jwt_api.dtos.EventResponseDTO;
import com.auth.jwt_api.exceptions.EventNotFoundException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private User organizer() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("org@example.com")
                .role(UserRole.ORGANIZER)
                .build();
    }

    @Test
    @DisplayName("create: mapeia os campos e associa o organizador autenticado")
    void create_shouldPersistEventWithOrganizer() {
        User organizer = organizer();
        EventRequestDTO request = new EventRequestDTO(
                "Rock Show", "desc", Instant.parse("2030-01-01T20:00:00Z"), "Arena");

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        EventResponseDTO result = eventService.create(request, organizer);

        assertThat(result.title()).isEqualTo("Rock Show");
        assertThat(result.location()).isEqualTo("Arena");
        assertThat(result.eventDate()).isEqualTo(Instant.parse("2030-01-01T20:00:00Z"));
        assertThat(result.organizerId()).isEqualTo(organizer.getId());
    }

    @Test
    @DisplayName("findById: lança EventNotFoundException quando o evento não existe")
    void findById_shouldThrow_whenMissing() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.findById(id));
    }

    @Test
    @DisplayName("search: aplica filtros/paginação e mapeia a página para DTO")
    void search_shouldMapPage() {
        User organizer = organizer();
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .title("E1")
                .location("L1")
                .eventDate(Instant.parse("2030-02-01T20:00:00Z"))
                .organizer(organizer)
                .build();

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponseDTO> result = eventService.search("L1", null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("E1");
        assertThat(result.getContent().get(0).organizerId()).isEqualTo(organizer.getId());
    }
}
