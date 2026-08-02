package com.eventhub.catalog_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank String name,
        String description,
        @NotBlank String venueName,
        @NotBlank String city,
        @NotNull @Future LocalDateTime eventDate
) {}