package com.seatsure.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * This service's own copy of the message contract published by the booking API. Keeping a
 * separate copy (instead of sharing a jar) lets the two services deploy independently;
 * unknown JSON fields are ignored, so the API can add fields without breaking this consumer.
 */
public record BookingEvent(
        String type,
        Long bookingId,
        Long userId,
        String customerName,
        String customerEmail,
        Long eventId,
        List<String> seatLabels,
        BigDecimal totalAmount,
        Instant occurredAt) {
}
