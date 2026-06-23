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

import com.auth.jwt_api.dtos.CheckoutResponseDTO;
import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.services.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
@Tag(name = "Pedidos", description = "Compra de ingressos e consulta dos pedidos do cliente")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Comprar ingressos de um lote (cliente)")
    public ResponseEntity<OrderResponseDTO> create(@RequestBody @Valid OrderRequestDTO request,
                                                   @AuthenticationPrincipal User customer) {
        OrderResponseDTO created = orderService.create(request, customer);
        return ResponseEntity.created(URI.create("/orders/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar meus pedidos")
    public ResponseEntity<List<OrderResponseDTO>> list(@AuthenticationPrincipal User customer) {
        return ResponseEntity.ok(orderService.findMyOrders(customer.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar meu pedido por id")
    public ResponseEntity<OrderResponseDTO> getById(@PathVariable UUID id,
                                                    @AuthenticationPrincipal User customer) {
        return ResponseEntity.ok(orderService.findMyOrder(id, customer.getId()));
    }

    @PostMapping("/{id}/checkout")
    @Operation(summary = "Iniciar pagamento do pedido (cliente dono) — cria PaymentIntent; pagamento é confirmado via webhook")
    public ResponseEntity<CheckoutResponseDTO> checkout(@PathVariable UUID id,
                                                        @AuthenticationPrincipal User customer) {
        return ResponseEntity.ok(orderService.checkout(id, customer));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar pedido (cliente dono) — PENDING -> CANCELLED, devolve assentos")
    public ResponseEntity<OrderResponseDTO> cancel(@PathVariable UUID id,
                                                   @AuthenticationPrincipal User customer) {
        return ResponseEntity.ok(orderService.cancel(id, customer));
    }

    @PostMapping("/{id}/refund")
    @Operation(summary = "Estornar pedido pago (cliente dono) — PAID -> REFUNDED, devolve assentos")
    public ResponseEntity<OrderResponseDTO> refund(@PathVariable UUID id,
                                                   @AuthenticationPrincipal User customer) {
        return ResponseEntity.ok(orderService.refund(id, customer));
    }
}
