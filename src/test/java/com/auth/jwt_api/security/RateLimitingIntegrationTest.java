package com.auth.jwt_api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.auth.jwt_api.exceptions.InvalidCredentialsException;
import com.auth.jwt_api.services.AuthService;
import com.auth.jwt_api.services.RefreshTokenService;

@SpringBootTest
@ActiveProfiles("test")
class RateLimitingIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;

    private static final String LOGIN_PATH = "/auth/login";
    private static final String REGISTER_PATH = "/auth/register";

    private static final String LOGIN_BODY = """
            {
              "email": "test@example.com",
              "password": "wrongPassword"
            }
            """;

    private static final String REGISTER_BODY = """
            {
              "email": "newuser@example.com",
              "password": "StrongPassword123",
              "role": "USER"
            }
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        when(authService.login(any())).thenThrow(new InvalidCredentialsException());
        doNothing().when(authService).register(any());
    }

    @Test
    @DisplayName("Rate Limit: deve retornar 429 após esgotar o limite de requisições em /auth/login")
    void rateLimiting_shouldReturn429_afterExceedingLimit() throws Exception {
        String ip = "10.0.0.1";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN_PATH)
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post(LOGIN_PATH)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Rate Limit: resposta 429 deve incluir o header Retry-After e body no formato ProblemDetail")
    void rateLimiting_shouldReturnRetryAfterHeaderAndProblemDetail_on429() throws Exception {
        String ip = "10.0.0.2";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN_PATH)
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post(LOGIN_PATH)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Too Many Requests"))
                .andExpect(jsonPath("$.retryAfter").isNumber());
    }

    @Test
    @DisplayName("Rate Limit: não deve ser aplicado em /auth/register mesmo com IP bloqueado em /auth/login")
    void rateLimiting_shouldNotApply_onRegisterRoute() throws Exception {
        String ip = "10.0.0.3";

        // Fase 1: esgotar o bucket para este IP na rota de login
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post(LOGIN_PATH)
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(LOGIN_BODY))
                    .andExpect(status().isUnauthorized());
        }

        // Confirmar que o IP está agora bloqueado em /auth/login
        mockMvc.perform(post(LOGIN_PATH)
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isTooManyRequests());

        // Fase 2: o mesmo IP deve aceder a /auth/register sem qualquer restrição
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(REGISTER_PATH)
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REGISTER_BODY))
                    .andExpect(status().isCreated());
        }
    }
}
