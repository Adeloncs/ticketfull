package com.auth.jwt_api.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.dtos.TransferTicketRequestDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.services.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ingressos", description = "Validação (check-in) de ingressos na portaria")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/{codeHash}/validate")
    @Operation(summary = "Validar ingresso (organizador do evento) — marca como USED")
    public ResponseEntity<TicketResponseDTO> validate(@PathVariable String codeHash,
                                                      @AuthenticationPrincipal User organizer) {
        return ResponseEntity.ok(ticketService.validate(codeHash, organizer));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transferir ingresso para outro usuário (detentor atual) — gera novo código")
    public ResponseEntity<TicketResponseDTO> transfer(@PathVariable UUID id,
                                                      @RequestBody @Valid TransferTicketRequestDTO request,
                                                      @AuthenticationPrincipal User requester) {
        return ResponseEntity.ok(ticketService.transfer(id, request.targetEmail(), requester));
    }
}
