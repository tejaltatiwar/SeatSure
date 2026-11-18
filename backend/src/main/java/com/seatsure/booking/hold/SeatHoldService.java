package com.seatsure.booking.hold;

import com.seatsure.common.config.AppProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Temporary seat locks in Redis. Each held seat is a key with a TTL, created with SET NX so
 * exactly one caller can win a seat even under heavy concurrency. The TTL means an abandoned
 * hold frees itself. This is the fast path; the unique index on booking_seats is the
 * database-level safety net behind it.
 */
@Service
public class SeatHoldService {

    private static final Logger log = LoggerFactory.getLogger(SeatHoldService.class);

    /** Delete the key only if it still belongs to the caller (compare-and-delete, atomic in Redis). */
    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration holdDuration;

    public SeatHoldService(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.holdDuration = properties.booking().seatHoldDuration();
    }

    /**
     * Tries to hold every seat for the user. All-or-nothing: if any seat is already held,
     * the seats acquired so far are released and false is returned.
     */
    public boolean tryHoldAll(Long eventId, Collection<Long> seatIds, Long userId) {
        List<Long> acquired = new ArrayList<>();
        for (Long seatId : seatIds) {
            Boolean won = redis.opsForValue().setIfAbsent(key(eventId, seatId), owner(userId), holdDuration);
            if (!Boolean.TRUE.equals(won)) {
                releaseAll(eventId, acquired, userId);
                return false;
            }
            acquired.add(seatId);
        }
        return true;
    }

    public void releaseAll(Long eventId, Collection<Long> seatIds, Long userId) {
        for (Long seatId : seatIds) {
            try {
                redis.execute(RELEASE_IF_OWNER, List.of(key(eventId, seatId)), owner(userId));
            } catch (RuntimeException ex) {
                // The TTL will clean up anyway; never fail a business operation over a release.
                log.warn("Could not release hold for event {} seat {}: {}", eventId, seatId, ex.getMessage());
            }
        }
    }

    private static String key(Long eventId, Long seatId) {
        return "seat-hold:" + eventId + ":" + seatId;
    }

    private static String owner(Long userId) {
        return String.valueOf(userId);
    }
}
