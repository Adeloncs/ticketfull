package com.auth.jwt_api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.dtos.CreateUserRequestDTO;
import com.auth.jwt_api.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Provisionamento de usuários com papéis privilegiados (ORGANIZER/ADMIN). Restrito a ADMIN no
 * {@code SecurityConfig}, já que o auto-registro público cria apenas CUSTOMER.
 */
@RestController
@RequestMapping("/admin/users")
@Tag(name = "Administração", description = "Gestão de usuários (apenas ADMIN)")
public class AdminUserController {

    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Criar usuário com papel definido (ADMIN)")
    public ResponseEntity<Void> create(@RequestBody @Valid CreateUserRequestDTO request) {
        authService.createUser(request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
