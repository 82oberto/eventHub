package com.eventhub.catalog_service.dto;

import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String name,
        String description,
        String venueName,
        String city,
        LocalDateTime eventDate,
        EventStatus status
) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getDescription(),
                e.getVenueName(), e.getCity(), e.getEventDate(), e.getStatus());
    }
}