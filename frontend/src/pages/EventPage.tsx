import { useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useCreateBooking, useEvent, useSeats } from '../api/queries'
import type { SeatAvailability } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { SeatMap } from '../components/SeatMap'
import { ErrorNotice, Loading } from '../components/StatusMessage'
import { MAX_SEATS_PER_BOOKING } from '../lib/constants'
import { formatEventDate, formatPrice } from '../lib/format'

export function EventPage() {
  const eventId = Number(useParams().eventId)
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()

  const event = useEvent(eventId)
  const seats = useSeats(eventId)
  const createBooking = useCreateBooking(eventId)
  const [selectedSeatIds, setSelectedSeatIds] = useState<ReadonlySet<number>>(new Set())

  const selectedSeats = useMemo(
    () => (seats.data ?? []).filter((seat) => selectedSeatIds.has(seat.seatId)),
    [seats.data, selectedSeatIds],
  )
  const total = selectedSeats.reduce((sum, seat) => sum + seat.price, 0)

  if (event.isPending || seats.isPending) return <Loading label="Loading seats" />
  if (event.error) return <ErrorNotice error={event.error} />
  if (seats.error) return <ErrorNotice error={seats.error} />

  function toggleSeat(seat: SeatAvailability) {
    createBooking.reset()
    setSelectedSeatIds((current) => {
      const next = new Set(current)
      if (next.has(seat.seatId)) next.delete(seat.seatId)
      else if (next.size < MAX_SEATS_PER_BOOKING) next.add(seat.seatId)
      return next
    })
  }

  function holdSeats() {
    if (!user) {
      navigate('/login', { state: { from: location.pathname } })
      return
    }
    createBooking.mutate([...selectedSeatIds], {
      onSuccess: (booking) => navigate(`/checkout/${booking.id}`),
      // Someone else got there first: drop our selection so the refreshed map starts clean.
      onError: (error) => {
        if (error instanceof ApiError && error.code === 'SEAT_UNAVAILABLE') setSelectedSeatIds(new Set())
      },
    })
  }

  return (
    <>
      <h1 className="text-3xl font-extrabold sm:text-4xl">{event.data.title}</h1>
      <p className="mt-2 text-ink-soft">
        {event.data.venue}, {event.data.city} · {formatEventDate(event.data.startsAt)}
      </p>

      {/* grid-cols-1 = minmax(0, 1fr): lets the seat map shrink and scroll inside its card instead of widening the page. */}
      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <SeatMap
          seats={seats.data}
          selectedSeatIds={selectedSeatIds}
          selectionFull={selectedSeatIds.size >= MAX_SEATS_PER_BOOKING}
          onToggleSeat={toggleSeat}
        />

        <aside className="h-fit rounded-xl bg-card p-5 lg:sticky lg:top-6" aria-live="polite">
          <h2 className="text-lg font-extrabold">Your seats</h2>
          {selectedSeats.length === 0 ? (
            <p className="mt-2 text-sm text-ink-soft">Select up to {MAX_SEATS_PER_BOOKING} seats on the map.</p>
          ) : (
            <>
              <p className="mt-2 font-medium">{selectedSeats.map((seat) => `${seat.rowLabel}${seat.seatNumber}`).join(', ')}</p>
              <p className="mt-3 flex justify-between border-t border-line pt-3 font-semibold">
                <span>Total</span>
                <span>{formatPrice(total)}</span>
              </p>
            </>
          )}

          {createBooking.error && (
            <div className="mt-4">
              <ErrorNotice error={createBooking.error} />
            </div>
          )}

          <button
            type="button"
            disabled={selectedSeats.length === 0 || createBooking.isPending}
            onClick={holdSeats}
            className="mt-4 w-full rounded-lg bg-ink px-4 py-3 font-semibold text-paper hover:bg-seat-free disabled:cursor-not-allowed disabled:bg-line disabled:text-ink-soft"
          >
            {createBooking.isPending ? 'Holding seats…' : user ? 'Hold seats for 5 minutes' : 'Sign in to hold seats'}
          </button>
        </aside>
      </div>
    </>
  )
}
