package com.eventhub.catalog_service.service;

import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}