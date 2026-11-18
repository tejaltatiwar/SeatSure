package com.seatsure.notification;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationListener.class);

    private final NotificationSender sender;

    public BookingNotificationListener(NotificationSender sender) {
        this.sender = sender;
    }

    @KafkaListener(topics = "${app.kafka.booking-events-topic}")
    public void onBookingEvent(BookingEvent event) {
        if (event == null) {
            log.warn("Skipping an undeserializable booking event");
            return;
        }
        Optional<String> subject = subjectFor(event);
        if (subject.isEmpty()) {
            log.debug("No notification defined for event type {}", event.type());
            return;
        }
        sender.send(event.customerEmail(), subject.get(), bodyFor(event));
    }

    private Optional<String> subjectFor(BookingEvent event) {
        return switch (event.type()) {
            case "BOOKING_CONFIRMED" -> Optional.of("Booking #" + event.bookingId() + " confirmed");
            case "BOOKING_EXPIRED" -> Optional.of("Your seat hold for booking #" + event.bookingId() + " expired");
            case "BOOKING_CANCELLED" -> Optional.of("Booking #" + event.bookingId() + " cancelled");
            default -> Optional.empty();
        };
    }

    private String bodyFor(BookingEvent event) {
        String seats = String.join(", ", event.seatLabels());
        return switch (event.type()) {
            case "BOOKING_CONFIRMED" -> "Hi %s, your seats (%s) are confirmed. Total paid: %s."
                    .formatted(event.customerName(), seats, event.totalAmount());
            case "BOOKING_EXPIRED" -> "Hi %s, we released seats %s because payment was not completed in time."
                    .formatted(event.customerName(), seats);
            default -> "Hi %s, your booking for seats %s was cancelled."
                    .formatted(event.customerName(), seats);
        };
    }
}
