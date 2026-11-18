package com.seatsure.booking;

import com.seatsure.auth.security.AuthenticatedUser;
import com.seatsure.booking.dto.BookingResponse;
import com.seatsure.booking.dto.CreateBookingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Holds the chosen seats and starts the payment countdown. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(user.userId(), request);
    }

    @GetMapping
    public List<BookingResponse> listMyBookings(@AuthenticationPrincipal AuthenticatedUser user) {
        return bookingService.listBookingsOf(user.userId());
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getMyBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                        @PathVariable Long bookingId) {
        return bookingService.getBookingOf(user.userId(), bookingId);
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@AuthenticationPrincipal AuthenticatedUser user,
                                         @PathVariable Long bookingId) {
        return bookingService.cancelBooking(user.userId(), bookingId);
    }
}
