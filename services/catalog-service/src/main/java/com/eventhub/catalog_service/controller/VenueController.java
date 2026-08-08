package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.dto.VenueResponse;
import com.eventhub.catalog_service.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueRepository venueRepository;

    @GetMapping
    public List<VenueResponse> getVenues() {
        return venueRepository.findAll(Sort.by("city", "name")).stream()
                .map(VenueResponse::from)
                .toList();
    }
}
