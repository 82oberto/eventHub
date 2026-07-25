package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.repository.EventRepository;
import com.eventhub.catalog_service.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}