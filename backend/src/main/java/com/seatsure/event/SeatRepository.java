package com.seatsure.event;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByEventIdOrderByRowLabelAscSeatNumberAsc(Long eventId);

    List<Seat> findByIdInAndEventId(Collection<Long> seatIds, Long eventId);

    @Query("""
            select s.event.id as eventId, count(s) as seatCount, min(s.price) as minPrice
            from Seat s
            where s.event.id in :eventIds
            group by s.event.id
            """)
    List<EventSeatStats> summarizeByEventIds(@Param("eventIds") Collection<Long> eventIds);
}
