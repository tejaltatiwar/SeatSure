package com.seatsure.booking;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically frees seats whose payment window has passed. Each booking is expired in its own
 * transaction (inside BookingService) so one failure cannot block the rest of the batch.
 */
@Component
public class BookingExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryScheduler.class);

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public BookingExpiryScheduler(BookingRepository bookingRepository, BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-sweep-interval-ms}")
    public void expireStaleBookings() {
        List<Booking> stale = bookingRepository
                .findTop100ByStatusAndHoldExpiresAtBeforeOrderByHoldExpiresAtAsc(
                        BookingStatus.PENDING_PAYMENT, Instant.now());
        for (Booking booking : stale) {
            try {
                bookingService.expireBooking(booking.getId());
            } catch (RuntimeException ex) {
                log.warn("Could not expire booking {}: {}", booking.getId(), ex.getMessage());
            }
        }
        if (!stale.isEmpty()) {
            log.info("Expiry sweep processed {} stale bookings", stale.size());
        }
    }
}
