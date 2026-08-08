package com.eventhub.catalog_service.dto;

import com.eventhub.catalog_service.entity.Venue;

import java.util.UUID;

public record VenueResponse(
        UUID id,
        String name,
        String city,
        String address
) {
    public static VenueResponse from(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getCity(), v.getAddress());
    }
}
