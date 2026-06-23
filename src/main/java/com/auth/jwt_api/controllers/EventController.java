package com.auth.jwt_api.controllers;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Listar eventos (público, paginado, filtros opcionais por local e data)")
    public ResponseEntity<PagedModel<EventResponseDTO>> list(
            @RequestParam(required = false) String location,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject Pageable pageable) {
        Page<EventResponseDTO> page = eventService.search(location, from, to, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar evento por id")
    public ResponseEntity<EventResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.findById(id));
    }
}
