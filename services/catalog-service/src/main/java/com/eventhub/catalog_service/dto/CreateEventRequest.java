package com.eventhub.catalog_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateEventRequest(
        @NotBlank String name,
        String description,
        @NotNull UUID venueId,
        @NotNull @Future LocalDateTime eventDate
) {}