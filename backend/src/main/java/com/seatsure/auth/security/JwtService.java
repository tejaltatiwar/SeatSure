package com.seatsure.auth.security;

import com.seatsure.common.config.AppProperties;
import com.seatsure.user.Role;
import com.seatsure.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies signed JWTs. Access and refresh tokens share a key but carry a
 * {@code type} claim so one can never be used in place of the other.
 */
@Service
public class JwtService {

    private static final String TYPE_CLAIM = "type";
    private static final String ROLE_CLAIM = "role";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;

    public JwtService(AppProperties properties) {
        byte[] secretBytes = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.security.jwt-secret must be at least 32 bytes long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtl = properties.security().accessTokenTtl();
        this.refreshTokenTtl = properties.security().refreshTokenTtl();
    }

    public String generateAccessToken(User user) {
        return buildToken(user, ACCESS, accessTokenTtl);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, REFRESH, refreshTokenTtl);
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    public AuthenticatedUser parseAccessToken(String token) {
        return parse(token, ACCESS);
    }

    public AuthenticatedUser parseRefreshToken(String token) {
        return parse(token, REFRESH);
    }

    private String buildToken(User user, String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TYPE_CLAIM, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey)
                .compact();
    }

    private AuthenticatedUser parse(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!expectedType.equals(claims.get(TYPE_CLAIM, String.class))) {
                throw new InvalidTokenException();
            }
            return new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException();
        }
    }
}
