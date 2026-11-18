package com.seatsure.booking;

import com.seatsure.booking.dto.SeatAvailabilityResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public seat map for an event (GET /api/v1/events/** is open in SecurityConfig). */
@RestController
@RequestMapping("/api/v1/events/{eventId}/seats")
public class SeatAvailabilityController {

    private final BookingService bookingService;

    public SeatAvailabilityController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<SeatAvailabilityResponse> getSeatMap(@PathVariable Long eventId) {
        return bookingService.getSeatAvailability(eventId);
    }
}
