package com.seatsure.common.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed view of every custom setting under the {@code app.*} prefix in application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Booking booking, Kafka kafka) {

    public record Security(
            String jwtSecret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            List<String> corsAllowedOrigins,
            int authRateLimitPerMinute) {
    }

    public record Booking(
            Duration seatHoldDuration,
            int maxSeatsPerBooking,
            long expirySweepIntervalMs) {
    }

    public record Kafka(String bookingEventsTopic) {
    }
}
