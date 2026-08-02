package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.dto.CreateEventRequest;
import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.repository.EventRepository;
import com.eventhub.catalog_service.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;

    @GetMapping
    public Page<EventResponse> getEvents(
            @RequestParam(required = false) String city,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable) {
        return eventService.search(city, pageable);
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable UUID id) {
        return eventService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.create(request);
    }
}