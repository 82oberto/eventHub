CREATE TABLE events (
                        id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        name        VARCHAR(255) NOT NULL,
                        description TEXT,
                        venue_name  VARCHAR(255) NOT NULL,
                        city        VARCHAR(100) NOT NULL,
                        event_date  TIMESTAMP NOT NULL,
                        status      VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
                        created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_city ON events(city);
CREATE INDEX idx_events_date ON events(event_date);