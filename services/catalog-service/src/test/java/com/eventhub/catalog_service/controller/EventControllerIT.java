package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.AbstractIntegrationTest;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.entity.Venue;
import com.eventhub.catalog_service.repository.EventRepository;
import com.eventhub.catalog_service.repository.EventSeatRepository;
import com.eventhub.catalog_service.repository.VenueRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the events API.
 *
 * <p>Runs the whole Spring context against a real PostgreSQL container: Flyway applies
 * V1-V4, so these assertions exercise controller, bean validation,
 * {@code GlobalExceptionHandler}, service, JPA and SQL together — including the venue
 * extraction and seat generation done by the V3/V4 migrations.
 *
 * <p>Each test runs in a transaction that is rolled back afterwards, so writes performed
 * by one test never leak into another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIT extends AbstractIntegrationTest {

    /** Seed data: 6 events (V2) across 6 distinct venues, 5 of them in Vienna. */
    private static final int SEEDED_EVENTS = 6;
    private static final int SEEDED_VIENNA_EVENTS = 5;
    private static final int SEEDED_VENUES = 6;

    /** V4 generates 5 rows x 10 seats per venue; rows A and B are PREMIUM. */
    private static final int SEATS_PER_VENUE = 50;
    private static final int PREMIUM_SEATS_PER_VENUE = 20;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    private Event anyEvent() {
        return eventRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst();
    }

    // ------------------------------------------------------------ seed data

    @Test
    @DisplayName("V2-V4 migrations seed events, venues and seats in the container database")
    void migrationsSeedTheDatabase() {
        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
        assertThat(venueRepository.count()).isEqualTo(SEEDED_VENUES);
        // one event_seat per seat of the event's venue, for every event
        assertThat(eventSeatRepository.count()).isEqualTo((long) SEEDED_EVENTS * SEATS_PER_VENUE);
    }

    @Test
    @DisplayName("V3 backfills every event with the venue extracted from the old columns")
    void everyEventIsLinkedToItsVenue() {
        assertThat(eventRepository.findAll())
                .isNotEmpty()
                .allSatisfy(event -> {
                    assertThat(event.getVenue()).isNotNull();
                    assertThat(event.getVenue().getName()).isNotBlank();
                    assertThat(event.getVenue().getCity()).isNotBlank();
                });
    }

    // -------------------------------------------------------- GET /events

    @Test
    @DisplayName("GET /events returns the seeded events sorted by date, with venue data flattened")
    void getEventsReturnsSeededEvents() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(SEEDED_EVENTS))
                .andExpect(jsonPath("$.content[0].name").value("Rock am Ring Warm-up"))
                .andExpect(jsonPath("$.content[0].venueName").value("Gasometer"))
                .andExpect(jsonPath("$.content[0].city").value("Vienna"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /events?city=vienna filters through the venue, case-insensitively")
    void getEventsFiltersByVenueCityIgnoringCase() throws Exception {
        mockMvc.perform(get("/events").param("city", "vienna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(SEEDED_VIENNA_EVENTS))
                .andExpect(jsonPath("$.content[*].city")
                        .value(Matchers.everyItem(Matchers.equalTo("Vienna"))));
    }

    @Test
    @DisplayName("GET /events?city=... returns an empty page for an unknown city")
    void getEventsReturnsEmptyPageForUnknownCity() throws Exception {
        mockMvc.perform(get("/events").param("city", "Atlantis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /events paginates the result set")
    void getEventsPaginates() throws Exception {
        mockMvc.perform(get("/events").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/events").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // 6 seeded events => pages 0..2 are full, page 3 is empty
        mockMvc.perform(get("/events").param("page", "3").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // ---------------------------------------------------- GET /events/{id}

    @Test
    @DisplayName("GET /events/{id} returns a single event with its venue")
    void getEventByIdReturnsEvent() throws Exception {
        Event existing = anyEvent();

        mockMvc.perform(get("/events/{id}", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId().toString()))
                .andExpect(jsonPath("$.name").value(existing.getName()))
                .andExpect(jsonPath("$.venueName").value(existing.getVenue().getName()))
                .andExpect(jsonPath("$.city").value(existing.getVenue().getCity()));
    }

    @Test
    @DisplayName("GET /events/{id} returns 404 with a problem detail for an unknown id")
    void getEventByIdReturnsNotFound() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/events/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Event not found: " + unknownId));
    }

    // -------------------------------------------- GET /events/{id}/seats

    @Test
    @DisplayName("GET /events/{id}/seats returns the seat map ordered by sector, row and number")
    void getSeatsReturnsOrderedSeatMap() throws Exception {
        Event existing = anyEvent();

        mockMvc.perform(get("/events/{id}/seats", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SEATS_PER_VENUE))
                .andExpect(jsonPath("$[0].sector").value("A"))
                .andExpect(jsonPath("$[0].rowLabel").value("A"))
                .andExpect(jsonPath("$[0].seatNumber").value(1))
                .andExpect(jsonPath("$[0].category").value("PREMIUM"))
                .andExpect(jsonPath("$[0].price").value(89.00))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[?(@.category == 'PREMIUM')]")
                        .value(Matchers.hasSize(PREMIUM_SEATS_PER_VENUE)));
    }

    @Test
    @DisplayName("GET /events/{id}/seats returns 404 for an unknown event")
    void getSeatsReturnsNotFoundForUnknownEvent() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/events/{id}/seats", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Event not found: " + unknownId));
    }

    // --------------------------------------- GET /events/{id}/availability

    @Test
    @DisplayName("GET /events/{id}/availability counts the seats still available")
    void getAvailabilityCountsAvailableSeats() throws Exception {
        Event existing = anyEvent();

        mockMvc.perform(get("/events/{id}/availability", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(SEATS_PER_VENUE));
    }

    // -------------------------------------------------------- POST /events

    @Test
    @DisplayName("POST /events creates an event for an existing venue and returns 201")
    void createEventReturnsCreated() throws Exception {
        Venue venue = venueRepository.findAll().getFirst();
        String eventDate = LocalDateTime.now().plusDays(45)
                .withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String body = """
                {
                  "name": "EventHub Launch Party",
                  "description": "Release celebration",
                  "venueId": "%s",
                  "eventDate": "%s"
                }
                """.formatted(venue.getId(), eventDate);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("EventHub Launch Party"))
                .andExpect(jsonPath("$.venueName").value(venue.getName()))
                .andExpect(jsonPath("$.city").value(venue.getCity()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS + 1);
        assertThat(eventRepository.findAll())
                .anyMatch(event -> event.getName().equals("EventHub Launch Party")
                        && event.getStatus() == EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("POST /events returns 404 when the venue does not exist")
    void createEventReturnsNotFoundForUnknownVenue() throws Exception {
        UUID unknownVenueId = UUID.randomUUID();
        String body = """
                {
                  "name": "Orphan Event",
                  "venueId": "%s",
                  "eventDate": "%s"
                }
                """.formatted(unknownVenueId, LocalDateTime.now().plusDays(10).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Venue not found: " + unknownVenueId));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }

    @Test
    @DisplayName("POST /events rejects a blank name with 400")
    void createEventRejectsBlankName() throws Exception {
        Venue venue = venueRepository.findAll().getFirst();
        String body = """
                {
                  "name": "  ",
                  "venueId": "%s",
                  "eventDate": "%s"
                }
                """.formatted(venue.getId(), LocalDateTime.now().plusDays(10).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("name")));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }

    @Test
    @DisplayName("POST /events rejects a missing venueId with 400")
    void createEventRejectsMissingVenueId() throws Exception {
        String body = """
                {
                  "name": "No Venue Event",
                  "eventDate": "%s"
                }
                """.formatted(LocalDateTime.now().plusDays(10).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("venueId")));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }

    @Test
    @DisplayName("POST /events rejects an event date in the past with 400")
    void createEventRejectsPastDate() throws Exception {
        Venue venue = venueRepository.findAll().getFirst();
        String body = """
                {
                  "name": "Yesterday's Concert",
                  "venueId": "%s",
                  "eventDate": "%s"
                }
                """.formatted(venue.getId(), LocalDateTime.now().minusDays(1).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("eventDate")));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }
}
