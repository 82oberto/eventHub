package com.eventhub.catalog_service.repository;

import com.eventhub.catalog_service.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}