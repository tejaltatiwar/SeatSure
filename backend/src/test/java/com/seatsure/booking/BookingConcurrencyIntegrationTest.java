package com.seatsure.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seatsure.booking.dto.BookingResponse;
import com.seatsure.booking.dto.CreateBookingRequest;
import com.seatsure.common.exception.BusinessRuleException;
import com.seatsure.common.exception.ConflictException;
import com.seatsure.event.dto.EventResponse;
import com.seatsure.support.AbstractIntegrationTest;
import com.seatsure.user.User;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

class BookingConcurrencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    @DisplayName("12 buyers racing for one seat: exactly one wins")
    void exactlyOneBuyerWinsTheSeat() throws Exception {
        EventResponse event = createEvent(1, 1);
        Long seatId = seatIdsOf(event.id()).get(0);
        int buyerCount = 12;
        List<User> buyers = IntStream.range(0, buyerCount).mapToObj(i -> createCustomer()).toList();

        ExecutorService pool = Executors.newFixedThreadPool(buyerCount);
        CountDownLatch allReady = new CountDownLatch(buyerCount);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> outcomes = new ArrayList<>();
        for (User buyer : buyers) {
            outcomes.add(pool.submit(() -> {
                allReady.countDown();
                startGun.await();
                try {
                    bookingService.createBooking(buyer.getId(), new CreateBookingRequest(event.id(), Set.of(seatId)));
                    return true;
                } catch (ConflictException seatTaken) {
                    return false;
                }
            }));
        }
        allReady.await();
        startGun.countDown();

        long winners = 0;
        for (Future<Boolean> outcome : outcomes) {
            if (outcome.get(15, TimeUnit.SECONDS)) {
                winners++;
            }
        }
        pool.shutdown();

        assertThat(winners).isEqualTo(1);
        assertThat(bookingSeatRepository.findActiveSeatIdsByEventId(event.id())).containsExactly(seatId);
    }

    @Test
    @DisplayName("database index still blocks a double booking when the Redis hold is lost")
    void databaseIndexIsTheSafetyNetBehindRedis() {
        EventResponse event = createEvent(1, 1);
        Long seatId = seatIdsOf(event.id()).get(0);
        User first = createCustomer();
        User second = createCustomer();
        bookingService.createBooking(first.getId(), new CreateBookingRequest(event.id(), Set.of(seatId)));

        // Simulate Redis being flushed or failing over: the hold disappears, Postgres must still protect the seat.
        redis.delete("seat-hold:" + event.id() + ":" + seatId);

        assertThatThrownBy(() ->
                bookingService.createBooking(second.getId(), new CreateBookingRequest(event.id(), Set.of(seatId))))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("code", "SEAT_UNAVAILABLE");
    }

    @Test
    @DisplayName("a failed multi-seat attempt leaves no seat held")
    void failedAttemptReleasesPartialHolds() {
        EventResponse event = createEvent(1, 3);
        List<Long> seatIds = seatIdsOf(event.id());
        User early = createCustomer();
        User late = createCustomer();
        bookingService.createBooking(early.getId(), new CreateBookingRequest(event.id(), Set.of(seatIds.get(2))));

        assertThatThrownBy(() ->
                bookingService.createBooking(late.getId(),
                        new CreateBookingRequest(event.id(), new LinkedHashSet<>(seatIds)))) // order A1, A2, A3: A3 fails last
                .isInstanceOf(ConflictException.class);

        // Seats 1 and 2 must be free again for someone else.
        BookingResponse retry = bookingService.createBooking(
                late.getId(), new CreateBookingRequest(event.id(), Set.of(seatIds.get(0), seatIds.get(1))));
        assertThat(retry.seatLabels()).containsExactly("A1", "A2");
    }

    @Test
    @DisplayName("more seats than the per-booking limit is rejected")
    void tooManySeatsRejected() {
        EventResponse event = createEvent(2, 4);
        User customer = createCustomer();

        assertThatThrownBy(() -> bookingService.createBooking(
                customer.getId(), new CreateBookingRequest(event.id(), Set.copyOf(seatIdsOf(event.id())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "TOO_MANY_SEATS");
    }

    @Test
    @DisplayName("cancelling a pending booking frees its seat for others")
    void cancelFreesSeat() {
        EventResponse event = createEvent(1, 1);
        Long seatId = seatIdsOf(event.id()).get(0);
        User first = createCustomer();
        User second = createCustomer();
        BookingResponse booking = bookingService.createBooking(
                first.getId(), new CreateBookingRequest(event.id(), Set.of(seatId)));

        bookingService.cancelBooking(first.getId(), booking.id());

        BookingResponse rebooked = bookingService.createBooking(
                second.getId(), new CreateBookingRequest(event.id(), Set.of(seatId)));
        assertThat(rebooked.status()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }
}
