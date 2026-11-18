package com.seatsure.booking.event;

import com.seatsure.common.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends booking events to Kafka strictly after the surrounding transaction commits.
 * Keyed by booking id so all events of one booking land on the same partition, in order.
 *
 * <p>Known trade-off: if the broker is down at that instant the message is logged and lost.
 * A transactional outbox table would close that gap; it is listed as a next step in the README.
 */
@Component
public class BookingEventKafkaForwarder {

    private static final Logger log = LoggerFactory.getLogger(BookingEventKafkaForwarder.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public BookingEventKafkaForwarder(KafkaTemplate<String, Object> kafkaTemplate, AppProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = properties.kafka().bookingEventsTopic();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void forward(BookingEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.bookingId()), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {} for booking {}", event.type(), event.bookingId(), error);
                    } else {
                        log.info("Published {} for booking {}", event.type(), event.bookingId());
                    }
                });
    }
}
