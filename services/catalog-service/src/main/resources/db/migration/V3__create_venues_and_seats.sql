-- ============ VENUES ============
CREATE TABLE venues (
                        id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name    VARCHAR(255) NOT NULL,
                        city    VARCHAR(100) NOT NULL,
                        address VARCHAR(255),
                        CONSTRAINT uq_venue_name_city UNIQUE (name, city)
);

-- Popola i venue dai dati già presenti negli eventi
INSERT INTO venues (name, city)
SELECT DISTINCT venue_name, city FROM events;

-- ============ EVENTS -> VENUES ============
ALTER TABLE events ADD COLUMN venue_id UUID;

UPDATE events e
SET venue_id = v.id
    FROM venues v
WHERE v.name = e.venue_name AND v.city = e.city;

ALTER TABLE events ALTER COLUMN venue_id SET NOT NULL;
ALTER TABLE events ADD CONSTRAINT fk_events_venue
    FOREIGN KEY (venue_id) REFERENCES venues(id);

ALTER TABLE events DROP COLUMN venue_name;
ALTER TABLE events DROP COLUMN city;

CREATE INDEX idx_events_venue ON events(venue_id);

-- ============ SEATS (posti fisici del venue) ============
CREATE TABLE seats (
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       venue_id    UUID NOT NULL REFERENCES venues(id),
                       sector      VARCHAR(20) NOT NULL,
                       row_label   VARCHAR(5)  NOT NULL,
                       seat_number INT         NOT NULL,
                       category    VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
                       CONSTRAINT uq_seat_position UNIQUE (venue_id, sector, row_label, seat_number)
);

CREATE INDEX idx_seats_venue ON seats(venue_id);

-- ============ EVENT_SEATS (disponibilità e prezzo per evento) ============
CREATE TABLE event_seats (
                             id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
                             seat_id  UUID NOT NULL REFERENCES seats(id),
                             price    NUMERIC(10,2) NOT NULL,
                             status   VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE',
                             version  BIGINT        NOT NULL DEFAULT 0,
                             CONSTRAINT uq_event_seat UNIQUE (event_id, seat_id)
);

CREATE INDEX idx_event_seats_event_status ON event_seats(event_id, status);