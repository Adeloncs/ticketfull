package com.auth.jwt_api.services;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.auth.jwt_api.dtos.AuthenticationRequestDTO;
import com.auth.jwt_api.dtos.RegisterRequestDTO;
import com.auth.jwt_api.exceptions.InvalidCredentialsException;
import com.auth.jwt_api.exceptions.UserAlreadyExistsException;
import com.auth.jwt_api.models.RefreshToken;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.UserRepository;
import com.auth.jwt_api.security.TokenService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpirationMs", 7_200_000L);
    }

    // -------------------------------------------------------------------------
    // Testes de register()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("register: deve codificar a password e guardar o utilizador")
    void register_shouldEncodePasswordAndSaveUser() {
        RegisterRequestDTO request = new RegisterRequestDTO("new@example.com", "plainPassword", UserRole.USER);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");

        authService.register(request);

        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: deve lançar UserAlreadyExistsException quando o email já existe")
    void register_shouldThrow_whenEmailAlreadyExists() {
        RegisterRequestDTO request = new RegisterRequestDTO("existing@example.com", "password", UserRole.USER);
        User existingUser = User.builder()
                .email("existing@example.com")
                .password("encoded")
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
    }

    // -------------------------------------------------------------------------
    // Testes de login()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("login: deve retornar LoginResult com accessToken quando as credenciais são válidas")
    void login_shouldReturnLoginResult_whenCredentialsAreValid() {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("user@example.com", "correctPassword");

        User user = User.builder()
                .email("user@example.com")
                .password("encoded")
                .role(UserRole.USER)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-uuid-value")
                .user(user)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(tokenService.generateToken(user)).thenReturn("access-jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthService.LoginResult result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-jwt-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-uuid-value");
        assertThat(result.expiresIn()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("login: deve lançar InvalidCredentialsException quando as credenciais são inválidas")
    void login_shouldThrow_whenCredentialsAreInvalid() {
        AuthenticationRequestDTO request = new AuthenticationRequestDTO("user@example.com", "wrongPassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}
