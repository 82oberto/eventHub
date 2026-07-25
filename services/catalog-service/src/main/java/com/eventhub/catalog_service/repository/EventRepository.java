package com.eventhub.catalog_service.repository;

import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    Page<Event> findByCityIgnoreCaseAndStatus(String city, EventStatus status, Pageable pageable);
}