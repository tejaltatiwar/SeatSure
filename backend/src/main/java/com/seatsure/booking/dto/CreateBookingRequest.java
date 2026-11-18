package com.seatsure.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record CreateBookingRequest(
        @NotNull Long eventId,
        @NotEmpty Set<@NotNull Long> seatIds) {
}
