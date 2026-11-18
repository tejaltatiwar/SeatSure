import { Link } from 'react-router-dom'
import { useBookings } from '../api/queries'
import type { Booking, BookingStatus } from '../api/types'
import { EmptyState, ErrorNotice, Loading } from '../components/StatusMessage'
import { formatEventDate, formatPrice, secondsUntil } from '../lib/format'

const STATUS_LABEL: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'Awaiting payment',
  CONFIRMED: 'Confirmed',
  EXPIRED: 'Hold expired',
  CANCELLED: 'Cancelled',
}

const STATUS_STYLE: Record<BookingStatus, string> = {
  PENDING_PAYMENT: 'bg-seat-picked text-ink',
  CONFIRMED: 'bg-ok text-white',
  EXPIRED: 'bg-seat-taken text-ink',
  CANCELLED: 'bg-seat-taken text-ink',
}

export function BookingsPage() {
  const { data: bookings, isPending, error } = useBookings()

  if (isPending) return <Loading label="Loading your bookings" />
  if (error) return <ErrorNotice error={error} />

  return (
    <>
      <h1 className="text-3xl font-extrabold sm:text-4xl">My bookings</h1>
      {bookings.length === 0 ? (
        <EmptyState title="No bookings yet" hint="Choose an event and pick seats to make your first booking." />
      ) : (
        <ul className="mt-6 flex flex-col gap-3">
          {bookings.map((booking) => (
            <BookingRow key={booking.id} booking={booking} />
          ))}
        </ul>
      )}
    </>
  )
}

function BookingRow({ booking }: { booking: Booking }) {
  const canStillPay = booking.status === 'PENDING_PAYMENT' && secondsUntil(booking.holdExpiresAt) > 0
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-card p-4">
      <div>
        <p className="font-semibold">Seats {booking.seatLabels.join(', ')}</p>
        <p className="text-sm text-ink-soft">
          Booking #{booking.id} · {formatEventDate(booking.createdAt)} · {formatPrice(booking.totalAmount)}
        </p>
      </div>
      <div className="flex items-center gap-3">
        <span className={`rounded-full px-3 py-1 text-sm font-medium ${STATUS_STYLE[booking.status]}`}>
          {STATUS_LABEL[booking.status]}
        </span>
        {canStillPay && (
          <Link to={`/checkout/${booking.id}`} className="rounded-lg bg-ink px-3 py-2 text-sm font-semibold text-paper">
            Pay now
          </Link>
        )}
      </div>
    </li>
  )
}
