package com.auth.jwt_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.auth.jwt_api.models.Event;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    List<Event> findByOrganizerId(UUID organizerId);

    Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);
}
