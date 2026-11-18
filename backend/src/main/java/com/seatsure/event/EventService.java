package com.seatsure.event;

import com.seatsure.common.exception.ResourceNotFoundException;
import com.seatsure.event.dto.CreateEventRequest;
import com.seatsure.event.dto.EventResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

    static final String UPCOMING_EVENTS_CACHE = "upcomingEvents";

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;

    public EventService(EventRepository eventRepository, SeatRepository seatRepository) {
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
    }

    /** Cached in Redis (60s TTL, see application.yml); evicted whenever an event is created. */
    @Cacheable(cacheNames = UPCOMING_EVENTS_CACHE, key = "#city == null ? 'all' : #city.toLowerCase()")
    @Transactional(readOnly = true)
    public List<EventResponse> listUpcomingEvents(String city) {
        Instant now = Instant.now();
        List<Event> events = (city == null || city.isBlank())
                ? eventRepository.findByStartsAtAfterOrderByStartsAtAsc(now)
                : eventRepository.findByCityIgnoreCaseAndStartsAtAfterOrderByStartsAtAsc(city.trim(), now);
        return new ArrayList<>(toResponses(events));
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId) {
        Event event = findEventOrThrow(eventId);
        return toResponses(List.of(event)).get(0);
    }

    @Transactional
    @CacheEvict(cacheNames = UPCOMING_EVENTS_CACHE, allEntries = true)
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = eventRepository.save(new Event(
                request.title().trim(),
                request.description(),
                request.venue().trim(),
                request.city().trim(),
                request.startsAt()));
        seatRepository.saveAll(generateSeatGrid(event, request));
        return toResponses(List.of(event)).get(0);
    }

    public Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
    }

    private List<Seat> generateSeatGrid(Event event, CreateEventRequest request) {
        List<Seat> seats = new ArrayList<>(request.rows() * request.seatsPerRow());
        for (int row = 0; row < request.rows(); row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            for (int number = 1; number <= request.seatsPerRow(); number++) {
                seats.add(new Seat(event, rowLabel, number, request.seatPrice()));
            }
        }
        return seats;
    }

    private List<EventResponse> toResponses(Collection<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, EventSeatStats> statsByEventId = seatRepository.summarizeByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventSeatStats::getEventId, Function.identity()));

        return events.stream().map(event -> {
            EventSeatStats stats = statsByEventId.get(event.getId());
            return new EventResponse(
                    event.getId(),
                    event.getTitle(),
                    event.getDescription(),
                    event.getVenue(),
                    event.getCity(),
                    event.getStartsAt(),
                    stats == null ? 0 : stats.getSeatCount(),
                    stats == null ? BigDecimal.ZERO : stats.getMinPrice());
        }).toList();
    }
}
