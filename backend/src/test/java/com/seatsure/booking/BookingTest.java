package com.seatsure.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seatsure.common.exception.BusinessRuleException;
import com.seatsure.event.Event;
import com.seatsure.event.Seat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant NOW = Instant.parse("2030-01-01T10:00:00Z");

    private final Event event = new Event("Show", null, "Hall", "Pune", NOW.plus(10, ChronoUnit.DAYS));

    private Booking pendingBookingExpiringIn(long minutes) {
        List<Seat> seats = List.of(
                new Seat(event, "A", 1, new BigDecimal("500.00")),
                new Seat(event, "A", 2, new BigDecimal("250.50")));
        return Booking.createPending(7L, 1L, seats, NOW.plus(minutes, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("total amount is the sum of the seat prices")
    void totalAmountIsSumOfSeatPrices() {
        Booking booking = pendingBookingExpiringIn(5);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("750.50");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("confirming inside the hold window succeeds")
    void confirmInsideHoldWindow() {
        Booking booking = pendingBookingExpiringIn(5);

        booking.confirm(NOW.plus(4, ChronoUnit.MINUTES));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("confirming after the hold window is rejected")
    void confirmAfterHoldWindowFails() {
        Booking booking = pendingBookingExpiringIn(5);

        assertThatThrownBy(() -> booking.confirm(NOW.plus(6, ChronoUnit.MINUTES)))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "HOLD_EXPIRED");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("expiring a pending booking frees all of its seats")
    void expireReleasesSeats() {
        Booking booking = pendingBookingExpiringIn(5);

        booking.expire();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(booking.getBookingSeats()).noneMatch(BookingSeat::isActive);
    }

    @Test
    @DisplayName("expiring a confirmed booking does nothing")
    void expireIgnoresConfirmedBooking() {
        Booking booking = pendingBookingExpiringIn(5);
        booking.confirm(NOW);

        booking.expire();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getBookingSeats()).allMatch(BookingSeat::isActive);
    }

    @Test
    @DisplayName("a confirmed booking cannot be cancelled")
    void cancelConfirmedBookingFails() {
        Booking booking = pendingBookingExpiringIn(5);
        booking.confirm(NOW);

        assertThatThrownBy(booking::cancel)
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "BOOKING_NOT_CANCELLABLE");
    }

    @Test
    @DisplayName("only the owner is recognised as owner")
    void ownership() {
        Booking booking = pendingBookingExpiringIn(5);

        assertThat(booking.isOwnedBy(7L)).isTrue();
        assertThat(booking.isOwnedBy(8L)).isFalse();
    }
}
