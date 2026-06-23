package com.auth.jwt_api.services;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.exceptions.InsufficientSeatsException;
import com.auth.jwt_api.exceptions.OrderNotFoundException;
import com.auth.jwt_api.exceptions.OrderNotPayableException;
import com.auth.jwt_api.exceptions.TicketBatchNotFoundException;
import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.repositories.OrderRepository;
import com.auth.jwt_api.repositories.TicketBatchRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TicketBatchRepository ticketBatchRepository;

    public OrderService(OrderRepository orderRepository, TicketBatchRepository ticketBatchRepository) {
        this.orderRepository = orderRepository;
        this.ticketBatchRepository = ticketBatchRepository;
    }

    @Transactional
    public OrderResponseDTO create(OrderRequestDTO request, User customer) {
        // Lock pessimista: serializa compradores concorrentes do mesmo lote
        TicketBatch batch = ticketBatchRepository.findByIdForUpdate(request.ticketBatchId())
                .orElseThrow(() -> new TicketBatchNotFoundException(request.ticketBatchId()));

        int quantity = request.quantity();
        if (batch.getAvailableSeats() < quantity) {
            throw new InsufficientSeatsException(batch.getAvailableSeats(), quantity);
        }
        batch.decreaseAvailableSeats(quantity);

        BigDecimal total = batch.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .customer(customer)
                .event(batch.getEvent())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .build();

        for (int i = 0; i < quantity; i++) {
            order.getTickets().add(Ticket.builder()
                    .order(order)
                    .ticketBatch(batch)
                    .codeHash(generateCodeHash())
                    .status(TicketStatus.VALID)
                    .build());
        }

        // cascade ALL em Order.tickets persiste os ingressos junto com o pedido
        return OrderResponseDTO.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDTO> findMyOrders(UUID customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(OrderResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findMyOrder(UUID id, UUID customerId) {
        return orderRepository.findByIdAndCustomerId(id, customerId)
                .map(OrderResponseDTO::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Confirma o pagamento (simulado) de um pedido PENDING do próprio cliente.
     * O lock pessimista impede que duas confirmações concorrentes processem o mesmo pedido.
     */
    @Transactional
    public OrderResponseDTO pay(UUID id, User customer) {
        Order order = orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPayableException(id, order.getStatus());
        }

        order.markAsPaid();
        return OrderResponseDTO.from(order);
    }

    /** Hash único que simula o QR Code do ingresso (SHA-256 de um UUID aleatório). */
    private String generateCodeHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
