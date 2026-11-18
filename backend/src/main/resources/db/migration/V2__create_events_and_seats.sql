CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description TEXT,
    venue       VARCHAR(200)  NOT NULL,
    city        VARCHAR(100)  NOT NULL,
    starts_at   TIMESTAMPTZ   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_starts_at ON events (starts_at);
CREATE INDEX idx_events_city ON events (city);

CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    event_id    BIGINT        NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    row_label   VARCHAR(3)    NOT NULL,
    seat_number INT           NOT NULL,
    price       NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    CONSTRAINT uq_seats_position UNIQUE (event_id, row_label, seat_number)
);

CREATE INDEX idx_seats_event_id ON seats (event_id);
