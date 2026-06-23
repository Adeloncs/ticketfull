package com.auth.jwt_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.dtos.EventResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.services.EventService;

@SpringBootTest
@ActiveProfiles("test")
class EventControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private EventService eventService;

    private MockMvc mockMvc;

    private static final String VALID_BODY = """
            { "title": "Show", "description": "d", "eventDate": "2030-01-01T20:00:00Z", "location": "Arena" }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private RequestPostProcessor as(UserRole role) {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").role(role).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("POST /events: ORGANIZER cria evento -> 201")
    void create_shouldReturn201_forOrganizer() throws Exception {
        EventResponseDTO dto = new EventResponseDTO(UUID.randomUUID(), "Show", "d",
                Instant.parse("2030-01-01T20:00:00Z"), "Arena", UUID.randomUUID(), null, null);
        when(eventService.create(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/events").with(as(UserRole.ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Show"));
    }

    @Test
    @DisplayName("POST /events: CUSTOMER -> 403 (role)")
    void create_shouldReturn403_forCustomer() throws Exception {
        mockMvc.perform(post("/events").with(as(UserRole.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /events: sem autenticação -> 401")
    void create_shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /events: corpo inválido (título/local vazios) -> 400")
    void create_shouldReturn400_whenBodyInvalid() throws Exception {
        String invalid = """
                { "title": "", "eventDate": "2030-01-01T20:00:00Z", "location": "" }
                """;
        mockMvc.perform(post("/events").with(as(UserRole.ORGANIZER))
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /events: público (sem autenticação), paginado -> 200")
    void list_shouldReturn200_public() throws Exception {
        when(eventService.search(any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());
    }
}
