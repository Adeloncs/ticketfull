package com.auth.jwt_api.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auth.jwt_api.dtos.CheckoutResponseDTO;
import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.events.OrderPaidEvent;
import com.auth.jwt_api.exceptions.EventNotAvailableException;
import com.auth.jwt_api.exceptions.InsufficientSeatsException;
import com.auth.jwt_api.exceptions.OrderNotCancellableException;
import com.auth.jwt_api.exceptions.OrderNotFoundException;
import com.auth.jwt_api.exceptions.OrderNotPayableException;
import com.auth.jwt_api.exceptions.OrderNotRefundableException;
import com.auth.jwt_api.exceptions.PaymentIntentNotFoundException;
import com.auth.jwt_api.exceptions.TicketBatchNotFoundException;
import com.auth.jwt_api.exceptions.TicketBatchNotOnSaleException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.EventStatus;
import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.payments.PaymentGateway;
import com.auth.jwt_api.payments.PaymentIntent;
import com.auth.jwt_api.repositories.OrderRepository;
import com.auth.jwt_api.repositories.TicketBatchRepository;

import io.micrometer.core.instrument.MeterRegistry;

@Service
public class OrderService {

    /** Nome da métrica de transições de pedido; o status vai como tag (PAID, EXPIRED, ...). */
    private static final String ORDERS_METRIC = "ticketfull.orders";

    private final OrderRepository orderRepository;
    private final TicketBatchRepository ticketBatchRepository;
    private final PaymentGateway paymentGateway;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration reservationDuration;

    public OrderService(OrderRepository orderRepository,
                        TicketBatchRepository ticketBatchRepository,
                        PaymentGateway paymentGateway,
                        MeterRegistry meterRegistry,
                        ApplicationEventPublisher eventPublisher,
                        @Value("${app.order.reservation-minutes:15}") long reservationMinutes) {
        this.orderRepository = orderRepository;
        this.ticketBatchRepository = ticketBatchRepository;
        this.paymentGateway = paymentGateway;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.reservationDuration = Duration.ofMinutes(reservationMinutes);
    }

    private void countTransition(OrderStatus status) {
        meterRegistry.counter(ORDERS_METRIC, "status", status.name()).increment();
    }

    @Transactional
    public OrderResponseDTO create(OrderRequestDTO request, User customer) {
        // Lock pessimista: serializa compradores concorrentes do mesmo lote
        TicketBatch batch = ticketBatchRepository.findByIdForUpdate(request.ticketBatchId())
                .orElseThrow(() -> new TicketBatchNotFoundException(request.ticketBatchId()));

        Event event = batch.getEvent();
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new EventNotAvailableException(event.getId());
        }
        if (!batch.isOnSale(Instant.now())) {
            throw new TicketBatchNotOnSaleException(batch.getId());
        }

        int quantity = request.quantity();
        if (batch.getAvailableSeats() < quantity) {
            throw new InsufficientSeatsException(batch.getAvailableSeats(), quantity);
        }
        batch.decreaseAvailableSeats(quantity);

        BigDecimal total = batch.getPrice().multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .customer(customer)
                .event(event)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .expiresAt(Instant.now().plus(reservationDuration))
                .build();

        for (int i = 0; i < quantity; i++) {
            order.getTickets().add(Ticket.builder()
                    .order(order)
                    .ticketBatch(batch)
                    .codeHash(CodeHashGenerator.generate())
                    .status(TicketStatus.VALID)
                    .build());
        }

        // cascade ALL em Order.tickets persiste os ingressos junto com o pedido
        Order saved = orderRepository.save(order);
        meterRegistry.counter("ticketfull.tickets.sold").increment(quantity);
        countTransition(OrderStatus.PENDING);
        return OrderResponseDTO.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> findMyOrders(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(OrderResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO findMyOrder(UUID id, UUID customerId) {
        return orderRepository.findByIdAndCustomerId(id, customerId)
                .map(OrderResponseDTO::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Inicia o pagamento de um pedido PENDING do próprio cliente: cria um PaymentIntent no gateway
     * e retorna os dados de checkout. A confirmação (PENDING -> PAID) ocorre depois, via webhook.
     * O lock pessimista evita criar dois PaymentIntents concorrentes para o mesmo pedido.
     */
    @Transactional
    public CheckoutResponseDTO checkout(UUID id, User customer) {
        Order order = orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotPayableException(id, order.getStatus());
        }
        if (order.isExpired()) {
            // Defesa contra a corrida em que o job de expiração ainda não rodou.
            throw new OrderNotPayableException(id);
        }

        PaymentIntent intent = paymentGateway.createPaymentIntent(order);
        order.assignPaymentIntent(intent.id());
        return CheckoutResponseDTO.of(order.getId(), intent);
    }

    /**
     * Confirma o pagamento a partir do webhook do gateway (PENDING -> PAID). Idempotente: se o pedido
     * já estiver PAID (webhook reentregue), não faz nada. O lock pessimista pelo PaymentIntent
     * serializa entregas concorrentes do mesmo evento.
     */
    @Transactional
    public void confirmPayment(String paymentIntentId) {
        Order order = orderRepository.findByPaymentIntentIdForUpdate(paymentIntentId)
                .orElseThrow(() -> new PaymentIntentNotFoundException(paymentIntentId));

        if (order.getStatus() == OrderStatus.PAID) {
            return; // já processado: webhook reentregue
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            // Pagamento chegou para um pedido já liberado (expirado/cancelado): exige estorno manual.
            throw new OrderNotPayableException(order.getId(), order.getStatus());
        }

        order.markAsPaid();
        countTransition(OrderStatus.PAID);
        // Notificação assíncrona (e-mail + QR) é disparada após o commit pelo listener.
        eventPublisher.publishEvent(new OrderPaidEvent(
                order.getId(), order.getCustomer().getEmail(), order.getTickets().size()));
    }

    /**
     * Cancela um pedido PENDING do próprio cliente, devolvendo os assentos ao lote.
     * Os ingressos do pedido são removidos (orphanRemoval).
     */
    @Transactional
    public OrderResponseDTO cancel(UUID id, User customer) {
        Order order = orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderNotCancellableException(id, order.getStatus());
        }

        releaseSeats(order);
        order.markAsCancelled();
        countTransition(OrderStatus.CANCELLED);
        return OrderResponseDTO.from(order);
    }

    /**
     * Estorna um pedido já PAID do próprio cliente: solicita o reembolso ao gateway, devolve os
     * assentos ao lote e marca o pedido como REFUNDED.
     */
    @Transactional
    public OrderResponseDTO refund(UUID id, User customer) {
        Order order = orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderNotRefundableException(id, order.getStatus());
        }

        if (order.getPaymentIntentId() != null) {
            paymentGateway.refund(order.getPaymentIntentId());
        }
        releaseSeats(order);
        order.markAsRefunded();
        countTransition(OrderStatus.REFUNDED);
        return OrderResponseDTO.from(order);
    }

    /** Ids de reservas PENDING já vencidas. Usado pelo scheduler; cada id é expirado em sua própria transação. */
    @Transactional(readOnly = true)
    public List<UUID> findOverdueOrderIds() {
        return orderRepository.findIdsByStatusAndExpiresAtBefore(OrderStatus.PENDING, Instant.now());
    }

    /**
     * Expira uma reserva PENDING vencida, devolvendo os assentos ao lote. Idempotente e defensivo:
     * revalida status e prazo sob lock, então um pedido pago/cancelado no intervalo é ignorado.
     */
    @Transactional
    public void expireOrder(UUID id) {
        Order order = orderRepository.findByIdForUpdate(id).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.PENDING || !order.isExpired()) {
            return;
        }
        releaseSeats(order);
        order.markAsExpired();
        countTransition(OrderStatus.EXPIRED);
    }

    /**
     * Devolve os assentos do pedido ao lote e remove os ingressos (orphanRemoval), preservando o
     * invariante {@code availableSeats + ingressos ativos == totalCapacity}. Compartilhado por
     * cancelamento, expiração e estorno.
     */
    private void releaseSeats(Order order) {
        if (order.getTickets().isEmpty()) {
            return;
        }
        UUID batchId = order.getTickets().get(0).getTicketBatch().getId();
        int quantity = order.getTickets().size();
        TicketBatch batch = ticketBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new TicketBatchNotFoundException(batchId));
        batch.increaseAvailableSeats(quantity);
        order.getTickets().clear();
    }
}
