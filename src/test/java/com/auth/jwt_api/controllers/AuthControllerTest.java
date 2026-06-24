package com.auth.jwt_api.controllers;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import jakarta.servlet.http.Cookie;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;
    private AuthService.LoginResult loginResult;
    private AuthService.LoginResult refreshResult;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        loginResult = new AuthService.LoginResult("access-jwt-token", "refresh-uuid-value", 7200L);
        refreshResult = new AuthService.LoginResult("new-access-jwt-token", "new-refresh-uuid-value", 7200L);
    }

    // -------------------------------------------------------------------------
    // POST /auth/register
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/register: deve retornar 201 Created com body válido")
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        doNothing().when(authService).register(any());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "newuser@example.com",
                                  "password": "StrongPassword123"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    // -------------------------------------------------------------------------
    // POST /auth/login — SUCESSO
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/login: deve retornar 200, accessToken no body e cookie refreshToken HttpOnly")
    void login_shouldReturn200WithAccessTokenAndRefreshTokenCookie() throws Exception {
        when(authService.login(any())).thenReturn(loginResult);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "correctPassword"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    // -------------------------------------------------------------------------
    // POST /auth/login — FALHA
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/login: deve retornar 401 ProblemDetail quando as credenciais são inválidas")
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrongPassword"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // POST /auth/refresh
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/refresh: deve retornar 200 e novo accessToken com cookie refreshToken presente")
    void refresh_shouldReturn200WithNewAccessToken_whenRefreshCookieIsPresent() throws Exception {
        when(authService.refreshToken(anyString())).thenReturn(refreshResult);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refresh-uuid-value")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                // Verifica que o cookie rotacionado é devolvido no header
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
    }

    // -------------------------------------------------------------------------
    // POST /auth/logout
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /auth/logout: deve retornar 204 No Content e limpar o cookie")
    void logout_shouldReturn204AndClearCookie() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refreshToken", "refresh-uuid-value")))
                .andExpect(status().isNoContent())
                // Verifica que o cookie é invalidado (MaxAge=0)
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
}
