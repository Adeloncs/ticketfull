package com.auth.jwt_api.controllers;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth.jwt_api.dtos.TicketBatchRequestDTO;
import com.auth.jwt_api.dtos.TicketBatchResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.services.TicketBatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events/{eventId}/ticket-batches")
@Tag(name = "Lotes de Ingressos", description = "Definição e consulta de lotes de um evento")
public class TicketBatchController {

    private final TicketBatchService ticketBatchService;

    public TicketBatchController(TicketBatchService ticketBatchService) {
        this.ticketBatchService = ticketBatchService;
    }

    @PostMapping
    @Operation(summary = "Adicionar lote ao evento (organizador dono do evento)")
    public ResponseEntity<TicketBatchResponseDTO> create(@PathVariable UUID eventId,
                                                         @RequestBody @Valid TicketBatchRequestDTO request,
                                                         @AuthenticationPrincipal User organizer) {
        TicketBatchResponseDTO created = ticketBatchService.addToEvent(eventId, request, organizer);
        return ResponseEntity
                .created(URI.create("/events/" + eventId + "/ticket-batches/" + created.id()))
                .body(created);
    }

    @GetMapping
    @Operation(summary = "Listar lotes de um evento")
    public ResponseEntity<List<TicketBatchResponseDTO>> list(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ticketBatchService.listByEvent(eventId));
    }
}
