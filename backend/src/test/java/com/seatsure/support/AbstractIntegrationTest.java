package com.seatsure.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.seatsure.event.EventService;
import com.seatsure.event.Seat;
import com.seatsure.event.SeatRepository;
import com.seatsure.event.dto.CreateEventRequest;
import com.seatsure.event.dto.EventResponse;
import com.seatsure.user.Role;
import com.seatsure.user.User;
import com.seatsure.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for tests that need the real thing: a throwaway PostgreSQL and Redis started by
 * Testcontainers, with Flyway building the schema from scratch exactly as in production.
 * The containers are started once per JVM and shared by every subclass (singleton pattern),
 * which keeps Spring's context cache valid. Kafka is mocked: broker behaviour is not under test.
 * Requires a running Docker daemon.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    private static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(REDIS_PORT);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        // No broker in tests: stop KafkaAdmin from waiting on a topic-creation call at startup.
        registry.add("spring.kafka.admin.auto-create", () -> "false");
    }

    @MockitoBean
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected EventService eventService;

    @Autowired
    protected SeatRepository seatRepository;

    @BeforeEach
    void stubKafka() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    protected User createCustomer() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        return userRepository.save(new User("Test User", email, "not-a-real-hash", Role.CUSTOMER));
    }

    protected EventResponse createEvent(int rows, int seatsPerRow) {
        return eventService.createEvent(new CreateEventRequest(
                "Test Event " + UUID.randomUUID(), "desc", "Test Hall", "Pune",
                Instant.now().plus(7, ChronoUnit.DAYS), rows, seatsPerRow, new BigDecimal("100.00")));
    }

    protected List<Long> seatIdsOf(Long eventId) {
        return seatRepository.findByEventIdOrderByRowLabelAscSeatNumberAsc(eventId).stream()
                .map(Seat::getId)
                .toList();
    }
}
