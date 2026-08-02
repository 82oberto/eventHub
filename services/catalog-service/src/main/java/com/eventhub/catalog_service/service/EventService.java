package com.eventhub.catalog_service.service;

import com.eventhub.catalog_service.dto.CreateEventRequest;
import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.exception.EventNotFoundException;
import com.eventhub.catalog_service.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public Page<EventResponse> search(String city, Pageable pageable) {
        Page<Event> page = (city != null && !city.isBlank())
                ? eventRepository.findByCityIgnoreCaseAndStatus(city, EventStatus.PUBLISHED, pageable)
                : eventRepository.findByStatus(EventStatus.PUBLISHED, pageable);
        return page.map(EventResponse::from);
    }

    public EventResponse getById(UUID id) {
        return eventRepository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenueName(request.venueName());
        event.setCity(request.city());
        event.setEventDate(request.eventDate());
        event.setStatus(EventStatus.PUBLISHED);
        return EventResponse.from(eventRepository.save(event));
    }
}