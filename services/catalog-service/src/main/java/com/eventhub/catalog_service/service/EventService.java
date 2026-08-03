package com.eventhub.catalog_service.service;

import com.eventhub.catalog_service.dto.CreateEventRequest;
import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.dto.SeatResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.entity.SeatStatus;
import com.eventhub.catalog_service.entity.Venue;
import com.eventhub.catalog_service.exception.EventNotFoundException;
import com.eventhub.catalog_service.exception.VenueNotFoundException;
import com.eventhub.catalog_service.repository.EventRepository;
import com.eventhub.catalog_service.repository.EventSeatRepository;
import com.eventhub.catalog_service.repository.VenueRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventSeatRepository eventSeatRepository;

    @Transactional(readOnly = true)
    public Page<EventResponse> search(String city, Pageable pageable) {
        Page<Event> page = (city != null && !city.isBlank())
                ? eventRepository.findByVenueCityIgnoreCaseAndStatus(city, EventStatus.PUBLISHED, pageable)
                : eventRepository.findByStatus(EventStatus.PUBLISHED, pageable);
        return page.map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        return eventRepository.findWithVenueById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeats(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new EventNotFoundException(eventId);
        }
        return eventSeatRepository.findByEventIdWithSeat(eventId).stream()
                .map(SeatResponse::from)
                .toList();
    }

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new VenueNotFoundException(request.venueId()));

        Event event = new Event();
        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(venue);
        event.setEventDate(request.eventDate());
        event.setStatus(EventStatus.PUBLISHED);

        return EventResponse.from(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public long countAvailableSeats(UUID eventId) {
        return eventSeatRepository.countByEventIdAndStatus(eventId, SeatStatus.AVAILABLE);
    }
}


