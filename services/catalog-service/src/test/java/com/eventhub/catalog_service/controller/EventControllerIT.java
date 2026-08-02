package com.eventhub.catalog_service.controller;

import com.eventhub.catalog_service.AbstractIntegrationTest;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.repository.EventRepository;
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
 * V1 (schema) and V2 (seed data), so the assertions below exercise controller, bean
 * validation, {@code GlobalExceptionHandler}, service, JPA and SQL together.
 *
 * <p>Each test runs in a transaction that is rolled back afterwards, so writes performed
 * by one test never leak into another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerIT extends AbstractIntegrationTest {

    /** Seed data in V2__seed_events.sql: 6 events, 5 of them in Vienna. */
    private static final int SEEDED_EVENTS = 6;
    private static final int SEEDED_VIENNA_EVENTS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("Flyway seed data is loaded into the container database")
    void seedDataIsLoaded() {
        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }

    @Test
    @DisplayName("GET /events returns the seeded events sorted by date")
    void getEventsReturnsSeededEvents() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(SEEDED_EVENTS))
                .andExpect(jsonPath("$.content[0].name").value("Rock am Ring Warm-up"))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /events?city=vienna filters case-insensitively")
    void getEventsFiltersByCityIgnoringCase() throws Exception {
        mockMvc.perform(get("/events").param("city", "vienna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(SEEDED_VIENNA_EVENTS))
                .andExpect(jsonPath("$.content[*].city").value(
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("Vienna"))));
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

    @Test
    @DisplayName("GET /events/{id} returns a single event")
    void getEventByIdReturnsEvent() throws Exception {
        Event existing = eventRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst();

        mockMvc.perform(get("/events/{id}", existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existing.getId().toString()))
                .andExpect(jsonPath("$.name").value(existing.getName()))
                .andExpect(jsonPath("$.venueName").value(existing.getVenueName()));
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

    @Test
    @DisplayName("POST /events creates an event and returns 201")
    void createEventReturnsCreated() throws Exception {
        String eventDate = LocalDateTime.now().plusDays(45)
                .withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String body = """
                {
                  "name": "EventHub Launch Party",
                  "description": "Release celebration",
                  "venueName": "Gasometer",
                  "city": "Vienna",
                  "eventDate": "%s"
                }
                """.formatted(eventDate);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("EventHub Launch Party"))
                .andExpect(jsonPath("$.city").value("Vienna"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS + 1);
        assertThat(eventRepository.findAll())
                .anyMatch(event -> event.getName().equals("EventHub Launch Party")
                        && event.getStatus() == EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("POST /events rejects a blank name with 400")
    void createEventRejectsBlankName() throws Exception {
        String body = """
                {
                  "name": "  ",
                  "venueName": "Gasometer",
                  "city": "Vienna",
                  "eventDate": "%s"
                }
                """.formatted(LocalDateTime.now().plusDays(10).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("name")));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }

    @Test
    @DisplayName("POST /events rejects an event date in the past with 400")
    void createEventRejectsPastDate() throws Exception {
        String body = """
                {
                  "name": "Yesterday's Concert",
                  "venueName": "Gasometer",
                  "city": "Vienna",
                  "eventDate": "%s"
                }
                """.formatted(LocalDateTime.now().minusDays(1).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("eventDate")));

        assertThat(eventRepository.count()).isEqualTo(SEEDED_EVENTS);
    }
}
