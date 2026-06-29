package com.auth.jwt_api.controllers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    private RequestPostProcessor as(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    @DisplayName("GET /me: usuário autenticado -> 200 com email e papel")
    void me_shouldReturnProfile_whenAuthenticated() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("organizador@ticketfull.com")
                .role(UserRole.ORGANIZER)
                .build();

        mockMvc.perform(get("/me").with(as(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("organizador@ticketfull.com"))
                .andExpect(jsonPath("$.role").value("ORGANIZER"));
    }

    @Test
    @DisplayName("GET /me: sem autenticação -> 401")
    void me_shouldReturn401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/me"))
                .andExpect(status().isUnauthorized());
    }
}
