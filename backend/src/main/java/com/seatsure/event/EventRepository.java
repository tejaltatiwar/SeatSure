package com.seatsure.event;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStartsAtAfterOrderByStartsAtAsc(Instant after);

    List<Event> findByCityIgnoreCaseAndStartsAtAfterOrderByStartsAtAsc(String city, Instant after);
}
