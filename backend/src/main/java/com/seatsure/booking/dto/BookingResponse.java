package com.seatsure.booking.dto;

import com.seatsure.booking.Booking;
import com.seatsure.booking.BookingSeat;
import com.seatsure.booking.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record BookingResponse(
        Long id,
        Long eventId,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant holdExpiresAt,
        Instant createdAt,
        List<String> seatLabels) {

    public static BookingResponse from(Booking booking) {
        List<String> labels = booking.getBookingSeats().stream()
                .map(BookingSeat::getSeat)
                .sorted(Comparator.comparing(com.seatsure.event.Seat::getRowLabel)
                        .thenComparingInt(com.seatsure.event.Seat::getSeatNumber))
                .map(com.seatsure.event.Seat::label)
                .toList();
        return new BookingResponse(
                booking.getId(),
                booking.getEventId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getHoldExpiresAt(),
                booking.getCreatedAt(),
                labels);
    }
}
