package com.seatsure.auth.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.seatsure.common.config.AppProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP rate limit on the auth endpoints (brute-force and credential-stuffing protection).
 * Limiters live in a bounded Caffeine cache so an attacker cannot grow memory by rotating IPs.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final SecurityErrorResponder errorResponder;
    private final RateLimiterConfig limiterConfig;
    private final Cache<String, RateLimiter> limitersByClient = Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_CLIENTS)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public AuthRateLimitFilter(AppProperties properties, SecurityErrorResponder errorResponder) {
        this.errorResponder = errorResponder;
        this.limiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(properties.security().authRateLimitPerMinute())
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTH_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = request.getRemoteAddr();
        RateLimiter limiter = limitersByClient.get(clientKey, key -> RateLimiter.of(key, limiterConfig));
        if (limiter.acquirePermission()) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setHeader("Retry-After", "60");
        errorResponder.write(request, response, HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMITED", "Too many attempts. Please wait a minute and try again.");
    }
}
