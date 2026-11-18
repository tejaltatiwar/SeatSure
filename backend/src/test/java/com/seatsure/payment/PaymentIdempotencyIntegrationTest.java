package com.seatsure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.seatsure.booking.BookingService;
import com.seatsure.booking.BookingStatus;
import com.seatsure.booking.dto.BookingResponse;
import com.seatsure.booking.dto.CreateBookingRequest;
import com.seatsure.booking.event.BookingEvent;
import com.seatsure.booking.event.BookingEventType;
import com.seatsure.common.exception.ConflictException;
import com.seatsure.common.exception.ResourceNotFoundException;
import com.seatsure.event.dto.EventResponse;
import com.seatsure.payment.dto.PayRequest;
import com.seatsure.payment.dto.PaymentResponse;
import com.seatsure.support.AbstractIntegrationTest;
import com.seatsure.user.User;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class PaymentIdempotencyIntegrationTest extends AbstractIntegrationTest {

    private static final PayRequest GOOD_CARD = new PayRequest("tok_visa");
    private static final PayRequest DECLINED_CARD = new PayRequest("tok_decline");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("repeating a payment with the same key charges once and returns the same result")
    void sameKeyChargesOnce() {
        User customer = createCustomer();
        BookingResponse booking = holdOneSeat(customer);
        String key = UUID.randomUUID().toString();

        PaymentResponse first = paymentService.pay(customer.getId(), booking.id(), key, GOOD_CARD);
        PaymentResponse replay = paymentService.pay(customer.getId(), booking.id(), key, GOOD_CARD);

        assertThat(first.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(replay.paymentId()).isEqualTo(first.paymentId());
        assertThat(replay.gatewayReference()).isEqualTo(first.gatewayReference());
        assertThat(paymentsFor(booking.id())).hasSize(1);
        assertThat(bookingService.getBookingOf(customer.getId(), booking.id()).status())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("a confirmed booking publishes exactly one BOOKING_CONFIRMED event")
    void confirmationPublishesOneEvent() {
        User customer = createCustomer();
        BookingResponse booking = holdOneSeat(customer);
        String key = UUID.randomUUID().toString();

        paymentService.pay(customer.getId(), booking.id(), key, GOOD_CARD);
        paymentService.pay(customer.getId(), booking.id(), key, GOOD_CARD);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(1)).send(eq("booking-events"), eq(String.valueOf(booking.id())), payload.capture());
        assertThat(payload.getValue()).isInstanceOfSatisfying(BookingEvent.class, event -> {
            assertThat(event.type()).isEqualTo(BookingEventType.BOOKING_CONFIRMED);
            assertThat(event.customerEmail()).isEqualTo(customer.getEmail());
        });
    }

    @Test
    @DisplayName("a declined card is recorded as FAILED and the booking stays payable")
    void declinedCardKeepsBookingPending() {
        User customer = createCustomer();
        BookingResponse booking = holdOneSeat(customer);

        PaymentResponse declined = paymentService.pay(customer.getId(), booking.id(), UUID.randomUUID().toString(), DECLINED_CARD);

        assertThat(declined.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(bookingService.getBookingOf(customer.getId(), booking.id()).status())
                .isEqualTo(BookingStatus.PENDING_PAYMENT);

        PaymentResponse retry = paymentService.pay(customer.getId(), booking.id(), UUID.randomUUID().toString(), GOOD_CARD);
        assertThat(retry.status()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("reusing a key for a different booking is a conflict")
    void keyReuseAcrossBookingsRejected() {
        User customer = createCustomer();
        BookingResponse firstBooking = holdOneSeat(customer);
        BookingResponse secondBooking = holdOneSeat(customer);
        String key = UUID.randomUUID().toString();
        paymentService.pay(customer.getId(), firstBooking.id(), key, GOOD_CARD);

        assertThatThrownBy(() -> paymentService.pay(customer.getId(), secondBooking.id(), key, GOOD_CARD))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "IDEMPOTENCY_KEY_REUSED");
    }

    @Test
    @DisplayName("paying for someone else's booking looks like a missing booking")
    void cannotPayForAnotherUsersBooking() {
        User owner = createCustomer();
        User stranger = createCustomer();
        BookingResponse booking = holdOneSeat(owner);

        assertThatThrownBy(() -> paymentService.pay(stranger.getId(), booking.id(), UUID.randomUUID().toString(), GOOD_CARD))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private BookingResponse holdOneSeat(User customer) {
        EventResponse event = createEvent(1, 1);
        Long seatId = seatIdsOf(event.id()).get(0);
        return bookingService.createBooking(customer.getId(), new CreateBookingRequest(event.id(), Set.of(seatId)));
    }

    private java.util.List<Payment> paymentsFor(Long bookingId) {
        return paymentRepository.findAll().stream()
                .filter(payment -> payment.getBookingId().equals(bookingId))
                .toList();
    }
}
