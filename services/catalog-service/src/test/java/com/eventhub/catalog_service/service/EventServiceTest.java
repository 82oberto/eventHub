package com.eventhub.catalog_service.service;

import com.eventhub.catalog_service.dto.CreateEventRequest;
import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.dto.SeatResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventSeat;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.entity.Seat;
import com.eventhub.catalog_service.entity.SeatCategory;
import com.eventhub.catalog_service.entity.SeatStatus;
import com.eventhub.catalog_service.entity.Venue;
import com.eventhub.catalog_service.exception.EventNotFoundException;
import com.eventhub.catalog_service.exception.VenueNotFoundException;
import com.eventhub.catalog_service.repository.EventRepository;
import com.eventhub.catalog_service.repository.EventSeatRepository;
import com.eventhub.catalog_service.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventService}. All repositories are mocked, so these tests cover
 * routing and mapping logic only and run without any database.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private EventSeatRepository eventSeatRepository;

    @InjectMocks
    private EventService eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private Venue sampleVenue() {
        Venue venue = new Venue();
        venue.setId(UUID.randomUUID());
        venue.setName("Wiener Stadthalle");
        venue.setCity("Vienna");
        return venue;
    }

    private Event sampleEvent(Venue venue) {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Vienna Symphony Night");
        event.setDescription("Classical concert");
        event.setVenue(venue);
        event.setEventDate(LocalDateTime.of(2026, 9, 15, 19, 30));
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    private EventSeat sampleEventSeat(Venue venue, String rowLabel, int number, SeatCategory category) {
        Seat seat = new Seat();
        seat.setId(UUID.randomUUID());
        seat.setVenue(venue);
        seat.setSector("A");
        seat.setRowLabel(rowLabel);
        seat.setSeatNumber(number);
        seat.setCategory(category);

        EventSeat eventSeat = new EventSeat();
        eventSeat.setId(UUID.randomUUID());
        eventSeat.setSeat(seat);
        eventSeat.setPrice(category == SeatCategory.PREMIUM
                ? new BigDecimal("89.00")
                : new BigDecimal("49.00"));
        eventSeat.setStatus(SeatStatus.AVAILABLE);
        return eventSeat;
    }

    // ---------------------------------------------------------------- search

    @Test
    @DisplayName("search without a city queries by status only")
    void searchWithoutCityQueriesByStatusOnly() {
        Event event = sampleEvent(sampleVenue());
        when(eventRepository.findByStatus(EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> result = eventService.search(null, PAGEABLE);

        assertThat(result.getContent())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(event.getId());
                    assertThat(response.name()).isEqualTo("Vienna Symphony Night");
                    assertThat(response.venueName()).isEqualTo("Wiener Stadthalle");
                    assertThat(response.city()).isEqualTo("Vienna");
                });
        verify(eventRepository, never())
                .findByVenueCityIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("search with a blank city is treated as no filter")
    void searchWithBlankCityIsTreatedAsNoFilter() {
        when(eventRepository.findByStatus(EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(sampleVenue()))));

        eventService.search("   ", PAGEABLE);

        verify(eventRepository).findByStatus(EventStatus.PUBLISHED, PAGEABLE);
        verify(eventRepository, never())
                .findByVenueCityIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("search with a city delegates to the case-insensitive venue-city query")
    void searchWithCityUsesVenueCityQuery() {
        when(eventRepository.findByVenueCityIgnoreCaseAndStatus("vienna", EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sampleEvent(sampleVenue()))));

        Page<EventResponse> result = eventService.search("vienna", PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByVenueCityIgnoreCaseAndStatus("vienna", EventStatus.PUBLISHED, PAGEABLE);
        verify(eventRepository, never()).findByStatus(any(), any());
    }

    // --------------------------------------------------------------- getById

    @Test
    @DisplayName("getById fetches the event together with its venue and maps it")
    void getByIdReturnsMappedEvent() {
        Venue venue = sampleVenue();
        Event event = sampleEvent(venue);
        when(eventRepository.findWithVenueById(event.getId())).thenReturn(Optional.of(event));

        EventResponse response = eventService.getById(event.getId());

        assertThat(response.id()).isEqualTo(event.getId());
        assertThat(response.name()).isEqualTo(event.getName());
        assertThat(response.venueName()).isEqualTo("Wiener Stadthalle");
        assertThat(response.city()).isEqualTo("Vienna");
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getById throws EventNotFoundException when the id is unknown")
    void getByIdThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(eventRepository.findWithVenueById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(unknownId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("create resolves the venue, maps every field and defaults the status to PUBLISHED")
    void createResolvesVenueAndMapsRequest() {
        Venue venue = sampleVenue();
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        CreateEventRequest request = new CreateEventRequest(
                "TechConf Austria 2026",
                "Annual software engineering conference",
                venue.getId(),
                eventDate);

        when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        EventResponse response = eventService.create(request);

        verify(eventRepository).save(eventCaptor.capture());
        Event persisted = eventCaptor.getValue();
        assertThat(persisted.getName()).isEqualTo("TechConf Austria 2026");
        assertThat(persisted.getDescription()).isEqualTo("Annual software engineering conference");
        assertThat(persisted.getVenue()).isSameAs(venue);
        assertThat(persisted.getEventDate()).isEqualTo(eventDate);
        assertThat(persisted.getStatus()).isEqualTo(EventStatus.PUBLISHED);

        assertThat(response.id()).isNotNull();
        assertThat(response.venueName()).isEqualTo("Wiener Stadthalle");
        assertThat(response.city()).isEqualTo("Vienna");
    }

    @Test
    @DisplayName("create throws VenueNotFoundException and saves nothing when the venue is unknown")
    void createThrowsWhenVenueMissing() {
        UUID unknownVenueId = UUID.randomUUID();
        CreateEventRequest request = new CreateEventRequest(
                "Orphan Event", null, unknownVenueId, LocalDateTime.now().plusDays(5));

        when(venueRepository.findById(unknownVenueId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(VenueNotFoundException.class)
                .hasMessageContaining(unknownVenueId.toString());

        verify(eventRepository, never()).save(any());
    }

    // -------------------------------------------------------------- getSeats

    @Test
    @DisplayName("getSeats maps event seats together with their physical seat data")
    void getSeatsMapsEventSeats() {
        UUID eventId = UUID.randomUUID();
        Venue venue = sampleVenue();
        when(eventRepository.existsById(eventId)).thenReturn(true);
        when(eventSeatRepository.findByEventIdWithSeat(eventId)).thenReturn(List.of(
                sampleEventSeat(venue, "A", 1, SeatCategory.PREMIUM),
                sampleEventSeat(venue, "C", 4, SeatCategory.STANDARD)));

        List<SeatResponse> seats = eventService.getSeats(eventId);

        assertThat(seats).hasSize(2);
        assertThat(seats.getFirst().sector()).isEqualTo("A");
        assertThat(seats.getFirst().rowLabel()).isEqualTo("A");
        assertThat(seats.getFirst().seatNumber()).isEqualTo(1);
        assertThat(seats.getFirst().category()).isEqualTo(SeatCategory.PREMIUM);
        assertThat(seats.getFirst().price()).isEqualByComparingTo("89.00");
        assertThat(seats.getFirst().status()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seats.get(1).category()).isEqualTo(SeatCategory.STANDARD);
        assertThat(seats.get(1).price()).isEqualByComparingTo("49.00");
    }

    @Test
    @DisplayName("getSeats throws EventNotFoundException before querying seats")
    void getSeatsThrowsWhenEventMissing() {
        UUID unknownId = UUID.randomUUID();
        when(eventRepository.existsById(unknownId)).thenReturn(false);

        assertThatThrownBy(() -> eventService.getSeats(unknownId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(unknownId.toString());

        verifyNoInteractions(eventSeatRepository);
    }

    // ---------------------------------------------------- countAvailableSeats

    @Test
    @DisplayName("countAvailableSeats counts only AVAILABLE seats for the event")
    void countAvailableSeatsCountsAvailableOnly() {
        UUID eventId = UUID.randomUUID();
        when(eventSeatRepository.countByEventIdAndStatus(eventId, SeatStatus.AVAILABLE)).thenReturn(42L);

        assertThat(eventService.countAvailableSeats(eventId)).isEqualTo(42L);
        verify(eventSeatRepository).countByEventIdAndStatus(eventId, SeatStatus.AVAILABLE);
    }
}
