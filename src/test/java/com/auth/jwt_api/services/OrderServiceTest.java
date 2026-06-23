package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth.jwt_api.dtos.CheckoutResponseDTO;
import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.exceptions.InsufficientSeatsException;
import com.auth.jwt_api.exceptions.OrderNotCancellableException;
import com.auth.jwt_api.exceptions.OrderNotFoundException;
import com.auth.jwt_api.exceptions.OrderNotPayableException;
import com.auth.jwt_api.exceptions.OrderNotRefundableException;
import com.auth.jwt_api.exceptions.PaymentIntentNotFoundException;
import com.auth.jwt_api.exceptions.TicketBatchNotFoundException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.Ticket;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.TicketStatus;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.payments.PaymentGateway;
import com.auth.jwt_api.payments.PaymentIntent;
import com.auth.jwt_api.repositories.OrderRepository;
import com.auth.jwt_api.repositories.TicketBatchRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketBatchRepository ticketBatchRepository;

    @Mock
    private PaymentGateway paymentGateway;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, ticketBatchRepository, paymentGateway, 15);
    }

    private User customer() {
        return User.builder().id(UUID.randomUUID()).email("cust@example.com").role(UserRole.CUSTOMER).build();
    }

    private TicketBatch batch(int availableSeats, String price) {
        Event event = Event.builder().id(UUID.randomUUID()).title("E").location("L").build();
        return TicketBatch.builder()
                .id(UUID.randomUUID())
                .event(event)
                .name("Pista")
                .price(new BigDecimal(price))
                .totalCapacity(availableSeats)
                .availableSeats(availableSeats)
                .build();
    }

    @Test
    @DisplayName("create: decrementa assentos, calcula total e gera N ingressos com hashes únicos")
    void create_shouldGenerateTicketsAndDecrementSeats() {
        User customer = customer();
        TicketBatch batch = batch(10, "100.00");
        OrderRequestDTO request = new OrderRequestDTO(batch.getId(), 3);

        when(ticketBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO result = orderService.create(request, customer);

        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(result.customerId()).isEqualTo(customer.getId());
        assertThat(result.tickets()).hasSize(3);
        assertThat(result.tickets()).extracting("codeHash").doesNotContainNull();
        assertThat(result.tickets()).extracting("codeHash").doesNotHaveDuplicates();
        // assentos decrementados no lote (10 - 3)
        assertThat(batch.getAvailableSeats()).isEqualTo(7);
    }

    @Test
    @DisplayName("create: lança InsufficientSeatsException e não persiste quando faltam assentos")
    void create_shouldThrow_whenNotEnoughSeats() {
        User customer = customer();
        TicketBatch batch = batch(1, "100.00");
        OrderRequestDTO request = new OrderRequestDTO(batch.getId(), 2);

        when(ticketBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));

        assertThrows(InsufficientSeatsException.class, () -> orderService.create(request, customer));
        assertThat(batch.getAvailableSeats()).isEqualTo(1); // inalterado
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("create: lança TicketBatchNotFoundException quando o lote não existe")
    void create_shouldThrow_whenBatchMissing() {
        User customer = customer();
        UUID batchId = UUID.randomUUID();
        OrderRequestDTO request = new OrderRequestDTO(batchId, 1);

        when(ticketBatchRepository.findByIdForUpdate(batchId)).thenReturn(Optional.empty());

        assertThrows(TicketBatchNotFoundException.class, () -> orderService.create(request, customer));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("findMyOrder: lança OrderNotFoundException quando não pertence ao cliente")
    void findMyOrder_shouldThrow_whenMissingForCustomer() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.findMyOrder(orderId, customerId));
    }

    private Order pendingOrder(User customer, OrderStatus status) {
        return pendingOrder(customer, status, Instant.now().plusSeconds(900));
    }

    private Order pendingOrder(User customer, OrderStatus status, Instant expiresAt) {
        return Order.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .event(Event.builder().id(UUID.randomUUID()).build())
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    @DisplayName("checkout: cria PaymentIntent e vincula ao pedido PENDING do próprio cliente")
    void checkout_shouldCreatePaymentIntent() {
        User customer = customer();
        Order order = pendingOrder(customer, OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));
        when(paymentGateway.createPaymentIntent(order))
                .thenReturn(new PaymentIntent("pi_123", "pi_123_secret", new BigDecimal("100.00"), "BRL"));

        CheckoutResponseDTO result = orderService.checkout(order.getId(), customer);

        assertThat(result.paymentIntentId()).isEqualTo("pi_123");
        assertThat(result.clientSecret()).isEqualTo("pi_123_secret");
        assertThat(order.getPaymentIntentId()).isEqualTo("pi_123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING); // ainda não pago
    }

    @Test
    @DisplayName("checkout: lança OrderNotFoundException quando o pedido não é do cliente/não existe")
    void checkout_shouldThrow_whenMissing() {
        User customer = customer();
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.checkout(id, customer));
    }

    @Test
    @DisplayName("checkout: lança OrderNotPayableException quando o pedido não está PENDING")
    void checkout_shouldThrow_whenNotPending() {
        User customer = customer();
        Order order = pendingOrder(customer, OrderStatus.PAID);
        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderNotPayableException.class, () -> orderService.checkout(order.getId(), customer));
        verify(paymentGateway, never()).createPaymentIntent(any());
    }

    @Test
    @DisplayName("checkout: lança OrderNotPayableException quando a reserva já expirou")
    void checkout_shouldThrow_whenExpired() {
        User customer = customer();
        Order order = pendingOrder(customer, OrderStatus.PENDING, Instant.now().minusSeconds(60));
        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderNotPayableException.class, () -> orderService.checkout(order.getId(), customer));
        verify(paymentGateway, never()).createPaymentIntent(any());
    }

    @Test
    @DisplayName("confirmPayment: confirma pedido PENDING a partir do webhook (-> PAID)")
    void confirmPayment_shouldMarkPaid() {
        User customer = customer();
        Order order = pendingOrder(customer, OrderStatus.PENDING);
        order.assignPaymentIntent("pi_abc");
        when(orderRepository.findByPaymentIntentIdForUpdate("pi_abc")).thenReturn(Optional.of(order));

        orderService.confirmPayment("pi_abc");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("confirmPayment: idempotente — webhook reentregue para pedido já PAID não falha nem reprocessa")
    void confirmPayment_shouldBeIdempotent() {
        User customer = customer();
        Order order = pendingOrder(customer, OrderStatus.PAID);
        order.assignPaymentIntent("pi_abc");
        when(orderRepository.findByPaymentIntentIdForUpdate("pi_abc")).thenReturn(Optional.of(order));

        orderService.confirmPayment("pi_abc"); // não lança

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("confirmPayment: lança PaymentIntentNotFoundException quando o PaymentIntent é desconhecido")
    void confirmPayment_shouldThrow_whenIntentUnknown() {
        when(orderRepository.findByPaymentIntentIdForUpdate("pi_x")).thenReturn(Optional.empty());

        assertThrows(PaymentIntentNotFoundException.class, () -> orderService.confirmPayment("pi_x"));
    }

    private TicketBatch batchWithSeats(int available, int total) {
        Event event = Event.builder().id(UUID.randomUUID()).title("E").location("L").build();
        return TicketBatch.builder()
                .id(UUID.randomUUID())
                .event(event)
                .name("Pista")
                .price(new BigDecimal("50.00"))
                .totalCapacity(total)
                .availableSeats(available)
                .build();
    }

    private Order orderWithTickets(User customer, OrderStatus status, TicketBatch batch, int qty) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .event(Event.builder().id(UUID.randomUUID()).build())
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .build();
        for (int i = 0; i < qty; i++) {
            order.getTickets().add(Ticket.builder()
                    .id(UUID.randomUUID())
                    .ticketBatch(batch)
                    .codeHash("hash-" + i)
                    .status(TicketStatus.VALID)
                    .build());
        }
        return order;
    }

    @Test
    @DisplayName("cancel: cancela pedido PENDING, devolve assentos ao lote e remove ingressos")
    void cancel_shouldRestoreSeatsAndClearTickets() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10); // 2 vendidos de 10
        Order order = orderWithTickets(customer, OrderStatus.PENDING, batch, 2);

        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));
        when(ticketBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));

        OrderResponseDTO result = orderService.cancel(order.getId(), customer);

        assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(batch.getAvailableSeats()).isEqualTo(10); // 8 + 2 devolvidos
        assertThat(order.getTickets()).isEmpty();
    }

    @Test
    @DisplayName("cancel: lança OrderNotCancellableException quando o pedido não está PENDING")
    void cancel_shouldThrow_whenNotPending() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10);
        Order order = orderWithTickets(customer, OrderStatus.PAID, batch, 2);

        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderNotCancellableException.class, () -> orderService.cancel(order.getId(), customer));
        assertThat(batch.getAvailableSeats()).isEqualTo(8); // inalterado
        verify(ticketBatchRepository, never()).findByIdForUpdate(batch.getId());
    }

    @Test
    @DisplayName("cancel: lança OrderNotFoundException quando o pedido não é do cliente/não existe")
    void cancel_shouldThrow_whenMissing() {
        User customer = customer();
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndCustomerIdForUpdate(id, customer.getId())).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.cancel(id, customer));
    }

    @Test
    @DisplayName("expireOrder: expira reserva PENDING vencida, devolve assentos e remove ingressos")
    void expireOrder_shouldReleaseSeats_whenOverdue() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10); // 2 retidos
        Order order = orderWithTickets(customer, OrderStatus.PENDING, batch, 2);
        order = Order.builder()
                .id(order.getId())
                .customer(customer)
                .event(order.getEvent())
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .expiresAt(Instant.now().minusSeconds(60))
                .tickets(order.getTickets())
                .build();
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(ticketBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));

        orderService.expireOrder(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(batch.getAvailableSeats()).isEqualTo(10);
        assertThat(order.getTickets()).isEmpty();
    }

    @Test
    @DisplayName("expireOrder: no-op quando o pedido não está mais PENDING (pago/cancelado no intervalo)")
    void expireOrder_shouldBeNoop_whenNotPending() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10);
        Order order = orderWithTickets(customer, OrderStatus.PAID, batch, 2);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        orderService.expireOrder(order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(batch.getAvailableSeats()).isEqualTo(8);
        verify(ticketBatchRepository, never()).findByIdForUpdate(batch.getId());
    }

    @Test
    @DisplayName("refund: estorna pedido PAID, chama o gateway, devolve assentos e marca REFUNDED")
    void refund_shouldRestoreSeatsAndCallGateway() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10);
        Order order = orderWithTickets(customer, OrderStatus.PAID, batch, 2);
        order.assignPaymentIntent("pi_ref");
        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));
        when(ticketBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));

        OrderResponseDTO result = orderService.refund(order.getId(), customer);

        assertThat(result.status()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(batch.getAvailableSeats()).isEqualTo(10);
        assertThat(order.getTickets()).isEmpty();
        verify(paymentGateway).refund("pi_ref");
    }

    @Test
    @DisplayName("refund: lança OrderNotRefundableException quando o pedido não está PAID")
    void refund_shouldThrow_whenNotPaid() {
        User customer = customer();
        TicketBatch batch = batchWithSeats(8, 10);
        Order order = orderWithTickets(customer, OrderStatus.PENDING, batch, 2);
        when(orderRepository.findByIdAndCustomerIdForUpdate(order.getId(), customer.getId()))
                .thenReturn(Optional.of(order));

        assertThrows(OrderNotRefundableException.class, () -> orderService.refund(order.getId(), customer));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(paymentGateway, never()).refund(any());
    }
}
