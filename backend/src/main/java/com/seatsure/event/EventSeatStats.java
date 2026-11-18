package com.seatsure.event;

import java.math.BigDecimal;

/** Spring Data projection: per-event seat count and cheapest seat price. */
public interface EventSeatStats {

    Long getEventId();

    long getSeatCount();

    BigDecimal getMinPrice();
}
