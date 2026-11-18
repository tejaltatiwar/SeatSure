package com.seatsure.payment;

import com.seatsure.booking.Booking;
import com.seatsure.booking.BookingRepository;
import com.seatsure.booking.event.BookingEventPublisher;
import com.seatsure.booking.event.BookingEventType;
import com.seatsure.common.exception.ConflictException;
import com.seatsure.common.exception.ResourceNotFoundException;
import com.seatsure.payment.dto.PayRequest;
import com.seatsure.payment.dto.PaymentResponse;
import com.seatsure.payment.gateway.PaymentGateway;
import com.seatsure.payment.gateway.PaymentResult;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pays for a pending booking. Idempotent: the client sends an Idempotency-Key, and repeating a
 * request with the same key returns the original outcome instead of charging again.
 *
 * <p>The booking row is locked for the duration, so two simultaneous requests for the same
 * booking run one after the other and the second one sees the first one's saved payment.
 * Trade-off: the provider call happens while the row lock is held, which is acceptable at this
 * scale because the lock is per booking and the call is bounded by retry and circuit breaker.
 */
@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final BookingEventPublisher eventPublisher;

    public PaymentService(BookingRepository bookingRepository,
                          PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          BookingEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentResponse pay(Long userId, Long bookingId, String idempotencyKey, PayRequest request) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .filter(found -> found.isOwnedBy(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Checked after taking the lock, so a concurrent duplicate that waited here sees the first result.
        Optional<Payment> previousAttempt = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (previousAttempt.isPresent()) {
            return replay(previousAttempt.get(), bookingId);
        }

        Instant now = Instant.now();
        booking.assertPayable(now);

        PaymentResult result = paymentGateway.charge(idempotencyKey, booking.getTotalAmount(), request.paymentToken());

        if (!result.approved()) {
            Payment failed = paymentRepository.save(new Payment(
                    bookingId, idempotencyKey, booking.getTotalAmount(), PaymentStatus.FAILED, null));
            return PaymentResponse.from(failed);
        }

        Payment succeeded = paymentRepository.save(new Payment(
                bookingId, idempotencyKey, booking.getTotalAmount(), PaymentStatus.SUCCEEDED, result.gatewayReference()));
        booking.confirm(now);
        eventPublisher.publish(BookingEventType.BOOKING_CONFIRMED, booking);
        return PaymentResponse.from(succeeded);
    }

    /** Same key must mean the same booking; otherwise the client is reusing a key by mistake. */
    private PaymentResponse replay(Payment previous, Long bookingId) {
        if (!previous.getBookingId().equals(bookingId)) {
            throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                    "This Idempotency-Key was already used for a different booking");
        }
        return PaymentResponse.from(previous);
    }
}
