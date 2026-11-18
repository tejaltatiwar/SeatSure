package com.seatsure.booking.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Message published to Kafka whenever a booking reaches a final state. Consumers (the
 * notification service) only need this payload, never a call back into the booking API.
 */
public record BookingEvent(
        BookingEventType type,
        Long bookingId,
        Long userId,
        String customerName,
        String customerEmail,
        Long eventId,
        List<String> seatLabels,
        BigDecimal totalAmount,
        Instant occurredAt) {
}
