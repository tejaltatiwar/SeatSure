package com.seatsure.event.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/** Admin request: creates an event together with a rectangular seat grid (rows A, B, C...). */
public record CreateEventRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @NotBlank @Size(max = 200) String venue,
        @NotBlank @Size(max = 100) String city,
        @NotNull @Future Instant startsAt,
        @Min(1) @Max(26) int rows,
        @Min(1) @Max(50) int seatsPerRow,
        @NotNull @DecimalMin("0.00") BigDecimal seatPrice) {
}
