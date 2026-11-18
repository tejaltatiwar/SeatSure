package com.seatsure.event.dto;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Serializable because event listings are cached in Redis. */
public record EventResponse(
        Long id,
        String title,
        String description,
        String venue,
        String city,
        Instant startsAt,
        long totalSeats,
        BigDecimal startingPrice) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
