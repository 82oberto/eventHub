package com.eventhub.catalog_service.repository;

import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = "venue")
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    Page<Event> findByVenueCityIgnoreCaseAndStatus(String city, EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "venue")
    Optional<Event> findWithVenueById(UUID id);
}