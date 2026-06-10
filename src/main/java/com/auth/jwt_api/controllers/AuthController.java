package com.auth.jwt_api.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.dtos.AuthenticationRequestDTO;
import com.auth.jwt_api.dtos.LoginResponseDTO;
import com.auth.jwt_api.dtos.RegisterRequestDTO;
import com.auth.jwt_api.services.AuthService;
import com.auth.jwt_api.services.AuthService.LoginResult;
import com.auth.jwt_api.services.RefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação e registro")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String COOKIE_PATH = "/auth";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Value("${api.security.token.refresh-expiration}")
    private long refreshExpirationMs;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", security = @SecurityRequirement(name = ""))
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid AuthenticationRequestDTO request,
                                                  HttpServletResponse response) {
        LoginResult result = authService.login(request);

        addRefreshTokenCookie(response, result.refreshToken());

        LoginResponseDTO body = new LoginResponseDTO(result.accessToken(), "Bearer", result.expiresIn());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar novo usuário", security = @SecurityRequirement(name = ""))
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token de acesso via refresh token", security = @SecurityRequirement(name = ""))
    public ResponseEntity<LoginResponseDTO> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE) String refreshToken,
            HttpServletResponse response) {
        LoginResult result = authService.refreshToken(refreshToken);

        addRefreshTokenCookie(response, result.refreshToken());

        LoginResponseDTO body = new LoginResponseDTO(result.accessToken(), "Bearer", result.expiresIn());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão e invalidar refresh token", security = @SecurityRequirement(name = ""))
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            refreshTokenService.deleteByToken(refreshToken);
        }

        clearRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(refreshExpirationMs / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
