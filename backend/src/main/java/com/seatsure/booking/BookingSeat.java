package com.seatsure.booking;

import com.seatsure.event.Seat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Links a booking to one seat. {@code active} is true while the booking owns the seat; the
 * partial unique index on (seat_id) WHERE active guarantees a seat has at most one owner.
 */
@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private boolean active = true;

    protected BookingSeat() {
        // required by JPA
    }

    BookingSeat(Booking booking, Seat seat) {
        this.booking = booking;
        this.seat = seat;
        this.eventId = seat.getEvent().getId();
    }

    public Seat getSeat() {
        return seat;
    }

    public boolean isActive() {
        return active;
    }

    void release() {
        this.active = false;
    }
}
