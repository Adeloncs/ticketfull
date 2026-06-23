package com.auth.jwt_api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.dtos.TicketResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.services.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
}
