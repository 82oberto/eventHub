package com.eventhub.catalog_service.repository;

import com.eventhub.catalog_service.entity.EventSeat;
import com.eventhub.catalog_service.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventSeatRepository extends JpaRepository<EventSeat, UUID> {

    @Query("""
        select es from EventSeat es
        join fetch es.seat s
        where es.event.id = :eventId
        order by s.sector, s.rowLabel, s.seatNumber
        """)
    List<EventSeat> findByEventIdWithSeat(@Param("eventId") UUID eventId);

    long countByEventIdAndStatus(UUID eventId, SeatStatus status);
}
