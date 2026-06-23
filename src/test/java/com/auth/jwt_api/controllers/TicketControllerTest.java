package com.auth.jwt_api.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.services.TicketService;

@SpringBootTest
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private TicketService ticketService;

    private MockMvc mockMvc;

    private static final String CODE = "abc123hash";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private RequestPostProcessor as(UserRole role) {
        User user = User.builder().id(UUID.randomUUID()).email("u@example.com").role(role).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("POST /tickets/{code}/validate: ORGANIZER valida -> 200, status USED")
    void validate_shouldReturn200_forOrganizer() throws Exception {
        TicketResponseDTO dto = new TicketResponseDTO(UUID.randomUUID(), UUID.randomUUID(), CODE, TicketStatus.USED);
        when(ticketService.validate(anyString(), any())).thenReturn(dto);

        mockMvc.perform(post("/tickets/{code}/validate", CODE).with(as(UserRole.ORGANIZER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
    }

    @Test
    @DisplayName("POST /tickets/{code}/validate: CUSTOMER -> 403 (role)")
    void validate_shouldReturn403_forCustomer() throws Exception {
        mockMvc.perform(post("/tickets/{code}/validate", CODE).with(as(UserRole.CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /tickets/{code}/validate: sem autenticação -> 401")
    void validate_shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(post("/tickets/{code}/validate", CODE))
                .andExpect(status().isUnauthorized());
    }
}
