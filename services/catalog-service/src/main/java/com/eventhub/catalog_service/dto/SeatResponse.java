package com.eventhub.catalog_service.dto;

import com.eventhub.catalog_service.entity.EventSeat;
import com.eventhub.catalog_service.entity.Seat;
import com.eventhub.catalog_service.entity.SeatCategory;
import com.eventhub.catalog_service.entity.SeatStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record SeatResponse(
        UUID id,
        String sector,
        String rowLabel,
        int seatNumber,
        SeatCategory category,
        BigDecimal price,
        SeatStatus status
) {
    public static SeatResponse from(EventSeat es) {
        Seat s = es.getSeat();
        return new SeatResponse(
                es.getId(),
                s.getSector(),
                s.getRowLabel(),
                s.getSeatNumber(),
                s.getCategory(),
                es.getPrice(),
                es.getStatus()
        );
    }
}