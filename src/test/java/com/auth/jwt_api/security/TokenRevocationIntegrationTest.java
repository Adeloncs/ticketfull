package com.auth.jwt_api.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;

/**
 * Valida a revogação de access token de ponta a ponta com beans reais (filtro de segurança +
 * lista de bloqueio + H2): após o logout, o mesmo token deixa de ser aceito.
 */
@SpringBootTest
@ActiveProfiles("test")
class TokenRevocationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Token revogado no logout é rejeitado em requisições subsequentes")
    void revokedTokenIsRejectedAfterLogout() throws Exception {
        String email = "revoke-" + System.nanoTime() + "@example.com";
        String credentials = "{ \"email\": \"" + email + "\", \"password\": \"secret123\" }";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginBody, "$.accessToken");
        String bearer = "Bearer " + token;

        // Token válido: rota autenticada responde 200
        mockMvc.perform(get("/orders").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        // Logout revoga o access token
        mockMvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        // Mesmo token agora é rejeitado
        mockMvc.perform(get("/orders").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
    }
}
