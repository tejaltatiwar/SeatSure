import { Link } from 'react-router-dom'
import type { EventSummary } from '../api/types'
import { formatEventDate, formatPrice } from '../lib/format'

/** An event shown as a ticket: details on the left, price and action on the tear-off stub. */
export function TicketCard({ event }: { event: EventSummary }) {
  return (
    <article className="ticket flex bg-card">
      <div className="min-w-0 flex-1 p-5">
        <h2 className="text-xl font-extrabold sm:text-2xl">{event.title}</h2>
        <p className="mt-1 text-ink-soft">
          {event.venue}, {event.city}
        </p>
        <p className="mt-3 font-medium">{formatEventDate(event.startsAt)}</p>
        {event.description && <p className="mt-2 max-w-prose text-sm text-ink-soft">{event.description}</p>}
      </div>
      <div className="ticket-stub flex flex-col items-start justify-between gap-3 p-4">
        <div>
          <p className="text-xs text-ink-soft">From</p>
          <p className="font-display text-xl font-extrabold">{formatPrice(event.startingPrice)}</p>
          <p className="text-xs text-ink-soft">{event.totalSeats} seats</p>
        </div>
        <Link
          to={`/events/${event.id}`}
          className="rounded-lg bg-ink px-3 py-2 text-sm font-semibold text-paper hover:bg-seat-free"
        >
          Choose seats
        </Link>
      </div>
    </article>
  )
}
