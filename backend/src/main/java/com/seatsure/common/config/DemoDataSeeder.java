package com.seatsure.common.config;

import com.seatsure.event.EventRepository;
import com.seatsure.event.EventService;
import com.seatsure.event.dto.CreateEventRequest;
import com.seatsure.user.Role;
import com.seatsure.user.User;
import com.seatsure.user.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Optional first-run data so the app is usable straight after `docker compose up`.
 * Off by default; enable with APP_SEED_ENABLED=true. Never enable in a real production database.
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private static final String ADMIN_EMAIL = "admin@seatsure.dev";
    private static final String CUSTOMER_EMAIL = "demo@seatsure.dev";

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataSeeder(UserRepository userRepository,
                          EventRepository eventRepository,
                          EventService eventService,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed.password:ChangeMe123!}") String demoPassword) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        createUserIfMissing("Demo Admin", ADMIN_EMAIL, Role.ADMIN);
        createUserIfMissing("Demo Customer", CUSTOMER_EMAIL, Role.CUSTOMER);

        if (eventRepository.count() == 0) {
            Instant base = Instant.now().plus(14, ChronoUnit.DAYS);
            eventService.createEvent(new CreateEventRequest(
                    "Arijit Singh Live", "An evening of Bollywood classics.",
                    "DY Patil Stadium", "Mumbai", base, 8, 12, new BigDecimal("1500.00")));
            eventService.createEvent(new CreateEventRequest(
                    "Stand-Up Night: Tech Edition", "Comedy for people who say \"it works on my machine\".",
                    "Phoenix Marketcity Arena", "Pune", base.plus(3, ChronoUnit.DAYS), 5, 10, new BigDecimal("599.00")));
            eventService.createEvent(new CreateEventRequest(
                    "Indie Rock Fest", "Three stages, twelve bands.",
                    "Palace Grounds", "Bengaluru", base.plus(10, ChronoUnit.DAYS), 10, 15, new BigDecimal("999.00")));
            log.info("Seeded demo events");
        }
    }

    private void createUserIfMissing(String name, String email, Role role) {
        if (!userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.save(new User(name, email, passwordEncoder.encode(demoPassword), role));
            log.info("Seeded demo user {}", email);
        }
    }
}
