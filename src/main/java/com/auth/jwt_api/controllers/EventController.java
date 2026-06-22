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

import com.auth.jwt_api.dtos.EventRequestDTO;
import com.auth.jwt_api.dtos.EventResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.services.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/events")
@Tag(name = "Eventos", description = "Criação e consulta de eventos")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Criar evento (organizador)")
    public ResponseEntity<EventResponseDTO> create(@RequestBody @Valid EventRequestDTO request,
                                                   @AuthenticationPrincipal User organizer) {
        EventResponseDTO created = eventService.create(request, organizer);
        return ResponseEntity.created(URI.create("/events/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar eventos")
    public ResponseEntity<List<EventResponseDTO>> list() {
        return ResponseEntity.ok(eventService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por id")
    public ResponseEntity<EventResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.findById(id));
    }
}
