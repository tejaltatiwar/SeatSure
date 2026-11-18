package com.seatsure.booking;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("select bs.seat.id from BookingSeat bs where bs.eventId = :eventId and bs.active = true")
    Set<Long> findActiveSeatIdsByEventId(@Param("eventId") Long eventId);
}
