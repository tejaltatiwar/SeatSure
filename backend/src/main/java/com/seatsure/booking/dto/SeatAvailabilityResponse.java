package com.seatsure.booking.dto;

import java.math.BigDecimal;

public record SeatAvailabilityResponse(
        Long seatId,
        String rowLabel,
        int seatNumber,
        BigDecimal price,
        boolean available) {
}
