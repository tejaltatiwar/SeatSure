package com.seatsure.booking;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"bookingSeats", "bookingSeats.seat"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"bookingSeats", "bookingSeats.seat"})
    Optional<Booking> findWithSeatsById(Long id);

    /** Row lock: serialises payment / cancel / expiry for the same booking. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    List<Booking> findTop100ByStatusAndHoldExpiresAtBeforeOrderByHoldExpiresAtAsc(
            BookingStatus status, Instant cutoff);
}
