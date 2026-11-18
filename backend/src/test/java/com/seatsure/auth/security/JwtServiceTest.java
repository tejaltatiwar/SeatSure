package com.seatsure.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.seatsure.common.config.AppProperties;
import com.seatsure.user.Role;
import com.seatsure.user.User;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(propertiesWithSecret(SECRET, Duration.ofMinutes(15)));
        user = new User("Asha", "asha@example.com", "hash", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    @DisplayName("access token round-trips user id and role")
    void accessTokenRoundTrip() {
        AuthenticatedUser identity = jwtService.parseAccessToken(jwtService.generateAccessToken(user));

        assertThat(identity.userId()).isEqualTo(42L);
        assertThat(identity.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("a refresh token cannot be used as an access token, and vice versa")
    void tokenTypesAreNotInterchangeable() {
        String refresh = jwtService.generateRefreshToken(user);
        String access = jwtService.generateAccessToken(user);

        assertThatThrownBy(() -> jwtService.parseAccessToken(refresh)).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.parseRefreshToken(access)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedTokenRejected() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("aa") ? "bb" : "aa");

        assertThatThrownBy(() -> jwtService.parseAccessToken(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("a token signed with another secret is rejected")
    void foreignSignatureRejected() {
        JwtService other = new JwtService(propertiesWithSecret("another-secret-another-secret-123456", Duration.ofMinutes(15)));

        assertThatThrownBy(() -> jwtService.parseAccessToken(other.generateAccessToken(user)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenRejected() {
        JwtService shortLived = new JwtService(propertiesWithSecret(SECRET, Duration.ofSeconds(-5)));

        assertThatThrownBy(() -> jwtService.parseAccessToken(shortLived.generateAccessToken(user)))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("a secret shorter than 32 bytes is refused at startup")
    void shortSecretRefused() {
        assertThatThrownBy(() -> new JwtService(propertiesWithSecret("too-short", Duration.ofMinutes(15))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static AppProperties propertiesWithSecret(String secret, Duration accessTtl) {
        return new AppProperties(
                new AppProperties.Security(secret, accessTtl, Duration.ofDays(7), List.of(), 20),
                null,
                null);
    }
}
