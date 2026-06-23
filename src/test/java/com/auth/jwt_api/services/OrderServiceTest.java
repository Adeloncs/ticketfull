package com.auth.jwt_api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auth.jwt_api.dtos.OrderRequestDTO;
import com.auth.jwt_api.dtos.OrderResponseDTO;
import com.auth.jwt_api.exceptions.InsufficientSeatsException;
import com.auth.jwt_api.exceptions.OrderNotFoundException;
import com.auth.jwt_api.exceptions.TicketBatchNotFoundException;
import com.auth.jwt_api.models.Event;
import com.auth.jwt_api.models.Order;
import com.auth.jwt_api.models.OrderStatus;
import com.auth.jwt_api.models.TicketBatch;
import com.auth.jwt_api.models.User;
import com.auth.jwt_api.models.UserRole;
import com.auth.jwt_api.repositories.OrderRepository;
import com.auth.jwt_api.repositories.TicketBatchRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketBatchRepository ticketBatchRepository;

    @InjectMocks
    private OrderService orderService;

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
}
