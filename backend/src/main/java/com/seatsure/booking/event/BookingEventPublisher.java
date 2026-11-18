package com.seatsure.booking.event;

import com.seatsure.booking.Booking;
import com.seatsure.booking.dto.BookingResponse;
import com.seatsure.common.exception.ResourceNotFoundException;
import com.seatsure.user.User;
import com.seatsure.user.UserRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Domain-side entry point for announcing booking outcomes. It only raises an in-process Spring
 * event; {@link BookingEventKafkaForwarder} ships it to Kafka after the DB transaction commits,
 * so a rolled-back booking never produces a message.
 */
@Component
public class BookingEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserRepository userRepository;

    public BookingEventPublisher(ApplicationEventPublisher applicationEventPublisher, UserRepository userRepository) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.userRepository = userRepository;
    }

    public void publish(BookingEventType type, Booking booking) {
        User customer = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", booking.getUserId()));
        BookingResponse summary = BookingResponse.from(booking);
        applicationEventPublisher.publishEvent(new BookingEvent(
                type,
                booking.getId(),
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                booking.getEventId(),
                summary.seatLabels(),
                booking.getTotalAmount(),
                Instant.now()));
    }
}
