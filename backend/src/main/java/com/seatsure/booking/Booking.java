package com.seatsure.booking;

import com.seatsure.common.exception.BusinessRuleException;
import com.seatsure.event.Seat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregate root of the booking flow. All state changes go through the methods below so the
 * lifecycle (PENDING_PAYMENT -> CONFIRMED | EXPIRED | CANCELLED) is enforced in one place.
 * {@code version} gives optimistic locking as a second guard against concurrent updates.
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "hold_expires_at", nullable = false)
    private Instant holdExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    private long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    protected Booking() {
        // required by JPA
    }

    public static Booking createPending(Long userId, Long eventId, Collection<Seat> seats, Instant holdExpiresAt) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.eventId = eventId;
        booking.status = BookingStatus.PENDING_PAYMENT;
        booking.holdExpiresAt = holdExpiresAt;
        booking.totalAmount = seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        seats.forEach(seat -> booking.bookingSeats.add(new BookingSeat(booking, seat)));
        return booking;
    }

    /** Throws unless the booking can still be paid: pending and inside its hold window. */
    public void assertPayable(Instant now) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("BOOKING_NOT_PAYABLE",
                    "Booking is " + status + " and can no longer be paid");
        }
        if (!holdExpiresAt.isAfter(now)) {
            throw new BusinessRuleException("HOLD_EXPIRED", "Your seat hold has expired. Please select seats again.");
        }
    }

    public void confirm(Instant now) {
        assertPayable(now);
        status = BookingStatus.CONFIRMED;
    }

    public void expire() {
        releaseIfPending(BookingStatus.EXPIRED);
    }

    public void cancel() {
        if (status != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessRuleException("BOOKING_NOT_CANCELLABLE", "Only unpaid bookings can be cancelled");
        }
        releaseIfPending(BookingStatus.CANCELLED);
    }

    private void releaseIfPending(BookingStatus target) {
        if (status != BookingStatus.PENDING_PAYMENT) {
            return;
        }
        status = target;
        bookingSeats.forEach(BookingSeat::release);
    }

    public boolean isOwnedBy(Long candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<BookingSeat> getBookingSeats() {
        return List.copyOf(bookingSeats);
    }
}
