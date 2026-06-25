package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.auth.jwt_api.dtos.EventRequestDTO;
import com.auth.jwt_api.dtos.EventResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.UserRepository;

/**
 * Verifica o ciclo de vida do evento contra o banco (H2): um evento DRAFT não aparece na busca
 * pública e passa a aparecer somente após ser publicado.
 */
@SpringBootTest
@ActiveProfiles("test")
class EventLifecycleIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Evento DRAFT fica oculto na busca pública até ser publicado")
    void draftHiddenUntilPublished() {
        User organizer = userRepository.save(User.builder()
                .email("org-" + UUID.randomUUID() + "@it.com").password("x").role(UserRole.ORGANIZER).build());
        String uniqueLocation = "Local-" + UUID.randomUUID();

        EventResponseDTO created = eventService.create(new EventRequestDTO(
                "Lifecycle Show", "desc", Instant.now().plus(Duration.ofDays(30)), uniqueLocation), organizer);

        // DRAFT: não aparece na busca pública
        assertThat(eventService.search(uniqueLocation, null, null, PageRequest.of(0, 10)).getTotalElements())
                .isZero();

        eventService.publish(created.id(), organizer);

        // PUBLISHED: agora aparece
        assertThat(eventService.search(uniqueLocation, null, null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
    }
}
