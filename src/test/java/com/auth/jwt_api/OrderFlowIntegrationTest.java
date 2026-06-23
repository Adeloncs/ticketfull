package com.auth.jwt_api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.auth.jwt_api.dtos.CheckoutResponseDTO;
import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.EventRepository;
import com.auth.jwt_api.repositories.OrderRepository;
import com.auth.jwt_api.repositories.TicketBatchRepository;
import com.auth.jwt_api.repositories.UserRepository;
import com.auth.jwt_api.services.OrderService;
import com.auth.jwt_api.support.TestcontainersConfiguration;

/**
 * Teste de integração ponta-a-ponta contra um PostgreSQL real (Testcontainers). Valida as migrações
 * Flyway (V1–V3), o mapeamento JPA (ddl-auto=validate), os locks pessimistas ({@code SELECT ... FOR UPDATE})
 * e o ciclo de vida completo do pedido: compra → checkout → webhook → estorno → expiração.
 *
 * <p>O intervalo do job de expiração é elevado para 1h para não competir com a expiração manual do teste.
 */
@SpringBootTest(properties = "app.order.expiration-scan-ms=3600000")
@Import(TestcontainersConfiguration.class)
class OrderFlowIntegrationTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private TicketBatchRepository ticketBatchRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Ciclo de vida do pedido em PostgreSQL real: compra, checkout, webhook, estorno e expiração")
    void fullOrderLifecycle_onRealPostgres() {
        User organizer = userRepository.save(User.builder()
                .email("org-" + UUID.randomUUID() + "@it.com").password("x").role(UserRole.ORGANIZER).build());
        User customer = userRepository.save(User.builder()
                .email("cust-" + UUID.randomUUID() + "@it.com").password("x").role(UserRole.CUSTOMER).build());
        Event event = eventRepository.save(Event.builder()
                .title("Show de Integração").eventDate(Instant.now().plus(Duration.ofDays(30)))
                .location("São Paulo").organizer(organizer).build());
        TicketBatch batch = ticketBatchRepository.save(TicketBatch.builder()
                .event(event).name("Pista").price(new BigDecimal("100.00"))
                .totalCapacity(10).availableSeats(10).build());
        UUID batchId = batch.getId();

        // 1. Compra: decrementa assentos no banco real e gera os ingressos com prazo de reserva
        OrderResponseDTO order = orderService.create(new OrderRequestDTO(batchId, 3), customer);
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.tickets()).hasSize(3);
        assertThat(order.expiresAt()).isNotNull();
        assertThat(availableSeats(batchId)).isEqualTo(7);

        // 2. Checkout + webhook: exercita o lock por PaymentIntent e a idempotência (PENDING -> PAID)
        CheckoutResponseDTO checkout = orderService.checkout(order.id(), customer);
        assertThat(checkout.paymentIntentId()).isNotBlank();
        orderService.confirmPayment(checkout.paymentIntentId());
        assertThat(status(order.id())).isEqualTo(OrderStatus.PAID);
        orderService.confirmPayment(checkout.paymentIntentId()); // reentrega: não deve falhar nem reprocessar
        assertThat(status(order.id())).isEqualTo(OrderStatus.PAID);

        // 3. Estorno: devolve assentos e marca REFUNDED
        orderService.refund(order.id(), customer);
        assertThat(status(order.id())).isEqualTo(OrderStatus.REFUNDED);
        assertThat(availableSeats(batchId)).isEqualTo(10);

        // 4. Expiração: nova reserva forçada a vencer; o job devolve os assentos
        OrderResponseDTO overdueOrder = orderService.create(new OrderRequestDTO(batchId, 2), customer);
        assertThat(availableSeats(batchId)).isEqualTo(8);
        jdbcTemplate.update("UPDATE orders SET expires_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(Duration.ofMinutes(1))), overdueOrder.id());

        List<UUID> overdue = orderService.findOverdueOrderIds();
        assertThat(overdue).contains(overdueOrder.id());
        overdue.forEach(orderService::expireOrder);

        assertThat(status(overdueOrder.id())).isEqualTo(OrderStatus.EXPIRED);
        assertThat(availableSeats(batchId)).isEqualTo(10);
    }

    private int availableSeats(UUID batchId) {
        return ticketBatchRepository.findById(batchId).orElseThrow().getAvailableSeats();
    }

    private OrderStatus status(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow().getStatus();
    }
}
