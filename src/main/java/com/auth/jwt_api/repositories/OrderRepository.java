package com.auth.jwt_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.auth.jwt_api.models.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerId(UUID customerId);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    /**
     * Carrega o pedido do cliente com lock pessimista de escrita, serializando
     * confirmações de pagamento concorrentes do mesmo pedido (evita duplo PAID).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.customer.id = :customerId")
    Optional<Order> findByIdAndCustomerIdForUpdate(@Param("id") UUID id, @Param("customerId") UUID customerId);
}
