/** Mirrors app.booking.max-seats-per-booking on the API; the API remains the source of truth. */
export const MAX_SEATS_PER_BOOKING = 6

/** Test payment tokens understood by the simulated gateway on the API. */
export const TEST_CARDS = [
  { token: 'tok_visa', label: 'Visa ending 4242', hint: 'Payment succeeds' },
  { token: 'tok_decline', label: 'Card that gets declined', hint: 'Shows the failed-payment path' },
  { token: 'tok_timeout', label: 'Payment provider outage', hint: 'Retries, then the circuit breaker opens' },
] as const
