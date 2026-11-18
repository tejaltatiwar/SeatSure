CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES users (id),
    event_id        BIGINT        NOT NULL REFERENCES events (id),
    status          VARCHAR(20)   NOT NULL,
    total_amount    NUMERIC(10,2) NOT NULL,
    hold_expires_at TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version         BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_pending_expiry ON bookings (hold_expires_at) WHERE status = 'PENDING_PAYMENT';

-- A seat can belong to at most one ACTIVE booking (pending or confirmed).
-- This partial unique index is the database-level safety net behind the Redis seat hold.
CREATE TABLE booking_seats (
    id         BIGSERIAL PRIMARY KEY,
    booking_id BIGINT  NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    seat_id    BIGINT  NOT NULL REFERENCES seats (id),
    event_id   BIGINT  NOT NULL REFERENCES events (id),
    active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uq_booking_seats_active_seat ON booking_seats (seat_id) WHERE active;
CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);
CREATE INDEX idx_booking_seats_event_active ON booking_seats (event_id) WHERE active;

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      BIGINT        NOT NULL REFERENCES bookings (id),
    idempotency_key VARCHAR(100)  NOT NULL,
    amount          NUMERIC(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    gateway_reference VARCHAR(100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX idx_payments_booking_id ON payments (booking_id);
