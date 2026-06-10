package com.auth.jwt_api.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";
    private static final long EXPIRATION_MS = 7_200_000L;

    private TokenService tokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", SECRET);
        ReflectionTestUtils.setField(tokenService, "expiration", EXPIRATION_MS);

        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .role(UserRole.USER)
                .build();
    }

    @Test
    @DisplayName("generateToken: deve retornar uma String JWT não vazia com formato válido")
    void generateToken_shouldReturnNonBlankJwtWithValidFormat() {
        String token = tokenService.generateToken(testUser);

        assertThat(token).isNotBlank();
        // JWT composto por 3 partes separadas por '.'
        assertThat(token.chars().filter(c -> c == '.').count()).isEqualTo(2);
    }

    @Test
    @DisplayName("validateToken: deve retornar o email do utilizador para um token válido")
    void validateToken_shouldReturnEmail_whenTokenIsValid() {
        String token = tokenService.generateToken(testUser);

        String subject = tokenService.validateToken(token);

        assertThat(subject).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("validateToken: deve retornar string vazia para um token com assinatura inválida")
    void validateToken_shouldReturnEmptyString_whenTokenIsInvalid() {
        String subject = tokenService.validateToken("invalid.token.value");

        assertThat(subject).isEmpty();
    }

    @Test
    @DisplayName("validateToken: deve retornar string vazia para um token expirado")
    void validateToken_shouldReturnEmptyString_whenTokenIsExpired() {
        // Injectar expiração negativa para gerar um token já expirado
        ReflectionTestUtils.setField(tokenService, "expiration", -1000L);
        String expiredToken = tokenService.generateToken(testUser);

        String subject = tokenService.validateToken(expiredToken);

        assertThat(subject).isEmpty();
    }
}
