package com.eventhub.catalog_service.service;

import com.eventhub.catalog_service.dto.CreateEventRequest;
import com.eventhub.catalog_service.dto.EventResponse;
import com.eventhub.catalog_service.entity.Event;
import com.eventhub.catalog_service.entity.EventStatus;
import com.eventhub.catalog_service.exception.EventNotFoundException;
import com.eventhub.catalog_service.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventService}. The repository is mocked, so these tests cover
 * routing/mapping logic only and run without any database.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    private Event sampleEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Vienna Symphony Night");
        event.setDescription("Classical concert");
        event.setVenueName("Wiener Stadthalle");
        event.setCity("Vienna");
        event.setEventDate(LocalDateTime.of(2026, 9, 15, 19, 30));
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }

    @Test
    @DisplayName("search without a city queries by status only")
    void searchWithoutCityQueriesByStatusOnly() {
        Event event = sampleEvent();
        when(eventRepository.findByStatus(EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> result = eventService.search(null, PAGEABLE);

        assertThat(result.getContent())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.id()).isEqualTo(event.getId());
                    assertThat(response.name()).isEqualTo("Vienna Symphony Night");
                    assertThat(response.city()).isEqualTo("Vienna");
                });
        verify(eventRepository, never())
                .findByCityIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("search with a blank city is treated as no filter")
    void searchWithBlankCityIsTreatedAsNoFilter() {
        when(eventRepository.findByStatus(EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sampleEvent())));

        eventService.search("   ", PAGEABLE);

        verify(eventRepository).findByStatus(EventStatus.PUBLISHED, PAGEABLE);
        verify(eventRepository, never())
                .findByCityIgnoreCaseAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("search with a city delegates to the case-insensitive city query")
    void searchWithCityUsesCityQuery() {
        when(eventRepository.findByCityIgnoreCaseAndStatus("vienna", EventStatus.PUBLISHED, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(sampleEvent())));

        Page<EventResponse> result = eventService.search("vienna", PAGEABLE);

        assertThat(result.getContent()).hasSize(1);
        verify(eventRepository).findByCityIgnoreCaseAndStatus("vienna", EventStatus.PUBLISHED, PAGEABLE);
        verify(eventRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("search maps entities to DTOs and never leaks the entity")
    void searchMapsEntitiesToDtos() {
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEvent())));

        Page<EventResponse> result = eventService.search(null, PAGEABLE);

        assertThat(result.getContent()).allSatisfy(response ->
                assertThat(response).isInstanceOf(EventResponse.class));
    }

    @Test
    @DisplayName("getById returns the mapped event when it exists")
    void getByIdReturnsMappedEvent() {
        Event event = sampleEvent();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

        EventResponse response = eventService.getById(event.getId());

        assertThat(response.id()).isEqualTo(event.getId());
        assertThat(response.name()).isEqualTo(event.getName());
        assertThat(response.venueName()).isEqualTo(event.getVenueName());
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    @DisplayName("getById throws EventNotFoundException when the id is unknown")
    void getByIdThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getById(unknownId))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }

    @Test
    @DisplayName("create maps every request field and defaults the status to PUBLISHED")
    void createMapsRequestAndDefaultsStatus() {
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);
        CreateEventRequest request = new CreateEventRequest(
                "TechConf Austria 2026",
                "Annual software engineering conference",
                "Austria Center Vienna",
                "Vienna",
                eventDate);

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
        assertThat(persisted.getVenueName()).isEqualTo("Austria Center Vienna");
        assertThat(persisted.getCity()).isEqualTo("Vienna");
        assertThat(persisted.getEventDate()).isEqualTo(eventDate);
        assertThat(persisted.getStatus()).isEqualTo(EventStatus.PUBLISHED);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("TechConf Austria 2026");
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
    }
}
