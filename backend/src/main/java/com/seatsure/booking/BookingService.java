package com.seatsure.booking;

import com.seatsure.booking.dto.BookingResponse;
import com.seatsure.booking.dto.CreateBookingRequest;
import com.seatsure.booking.dto.SeatAvailabilityResponse;
import com.seatsure.booking.event.BookingEventPublisher;
import com.seatsure.booking.event.BookingEventType;
import com.seatsure.booking.hold.SeatHoldService;
import com.seatsure.common.config.AppProperties;
import com.seatsure.common.exception.BusinessRuleException;
import com.seatsure.common.exception.ConflictException;
import com.seatsure.common.exception.ResourceNotFoundException;
import com.seatsure.event.Event;
import com.seatsure.event.EventService;
import com.seatsure.event.Seat;
import com.seatsure.event.SeatRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking use cases. Seat safety has two layers:
 * <ol>
 *   <li>Redis hold (SET NX + TTL): cheap, fast, rejects most contention before touching Postgres.</li>
 *   <li>Partial unique index on booking_seats(seat_id) WHERE active: the authoritative guarantee
 *       that a seat never has two active owners, even if Redis is flushed or misbehaves.</li>
 * </ol>
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final EventService eventService;
    private final SeatHoldService seatHoldService;
    private final BookingEventPublisher eventPublisher;
    private final AppProperties.Booking bookingSettings;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          SeatRepository seatRepository,
                          EventService eventService,
                          SeatHoldService seatHoldService,
                          BookingEventPublisher eventPublisher,
                          AppProperties properties) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.eventService = eventService;
        this.seatHoldService = seatHoldService;
        this.eventPublisher = eventPublisher;
        this.bookingSettings = properties.booking();
    }

    @Transactional(readOnly = true)
    public List<SeatAvailabilityResponse> getSeatAvailability(Long eventId) {
        eventService.findEventOrThrow(eventId);
        Set<Long> takenSeatIds = bookingSeatRepository.findActiveSeatIdsByEventId(eventId);
        return seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(eventId).stream()
                .map(seat -> new SeatAvailabilityResponse(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getSeatNumber(),
                        seat.getPrice(),
                        !takenSeatIds.contains(seat.getId())))
                .toList();
    }

    @Transactional
    public BookingResponse createBooking(Long userId, CreateBookingRequest request) {
        Set<Long> seatIds = request.seatIds();
        if (seatIds.size() > bookingSettings.maxSeatsPerBooking()) {
            throw new BusinessRuleException("TOO_MANY_SEATS",
                    "You can book at most " + bookingSettings.maxSeatsPerBooking() + " seats at a time");
        }

        Event event = eventService.findEventOrThrow(request.eventId());
        if (!event.getStartsAt().isAfter(Instant.now())) {
            throw new BusinessRuleException("EVENT_ALREADY_STARTED", "Bookings for this event are closed");
        }

        List<Seat> seats = seatRepository.findByIdInAndEventId(seatIds, event.getId());
        if (seats.size() != seatIds.size()) {
            throw new BusinessRuleException("INVALID_SEATS", "One or more seats do not belong to this event");
        }

        if (!seatHoldService.tryHoldAll(event.getId(), seatIds, userId)) {
            throw seatUnavailable();
        }

        try {
            Instant holdExpiresAt = Instant.now().plus(bookingSettings.seatHoldDuration());
            Booking booking = Booking.createPending(userId, event.getId(), seats, holdExpiresAt);
            bookingRepository.saveAndFlush(booking);
            return BookingResponse.from(booking);
        } catch (DataIntegrityViolationException seatAlreadyOwned) {
            seatHoldService.releaseAll(event.getId(), seatIds, userId);
            throw seatUnavailable();
        } catch (RuntimeException unexpected) {
            seatHoldService.releaseAll(event.getId(), seatIds, userId);
            throw unexpected;
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listBookingsOf(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingOf(Long userId, Long bookingId) {
        return BookingResponse.from(findOwnedBooking(userId, bookingId));
    }

    @Transactional
    public BookingResponse cancelBooking(Long userId, Long bookingId) {
        Booking booking = lockOwnedBooking(userId, bookingId);
        booking.cancel();
        releaseSeatHolds(booking);
        eventPublisher.publish(BookingEventType.BOOKING_CANCELLED, booking);
        return BookingResponse.from(booking);
    }

    /**
     * Called by the expiry scheduler. Safe to run on several instances at once: the row lock
     * serialises them and the status re-check makes the second attempt a no-op.
     */
    @Transactional
    public void expireBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null
                || booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getHoldExpiresAt().isAfter(Instant.now())) {
            return;
        }
        booking.expire();
        releaseSeatHolds(booking);
        eventPublisher.publish(BookingEventType.BOOKING_EXPIRED, booking);
    }

    private Booking findOwnedBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findWithSeatsById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return requireOwner(booking, userId);
    }

    private Booking lockOwnedBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return requireOwner(booking, userId);
    }

    /** Someone else's booking is reported as "not found" so ids cannot be probed. */
    private Booking requireOwner(Booking booking, Long userId) {
        if (!booking.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Booking", booking.getId());
        }
        return booking;
    }

    private void releaseSeatHolds(Booking booking) {
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bookingSeat -> bookingSeat.getSeat().getId())
                .toList();
        seatHoldService.releaseAll(booking.getEventId(), seatIds, booking.getUserId());
    }

    private static ConflictException seatUnavailable() {
        return new ConflictException("SEAT_UNAVAILABLE", "One or more selected seats were just taken. Please pick others.");
    }
}
