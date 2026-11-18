import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useBooking, useCancelBooking, usePay } from '../api/queries'
import { ErrorNotice, Loading } from '../components/StatusMessage'
import { TEST_CARDS } from '../lib/constants'
import { formatCountdown, formatPrice } from '../lib/format'
import { newIdempotencyKey } from '../lib/idempotencyKey'
import { useCountdown } from '../lib/useCountdown'

export function CheckoutPage() {
  const bookingId = Number(useParams().bookingId)
  const booking = useBooking(bookingId)
  const pay = usePay(bookingId)
  const cancel = useCancelBooking(bookingId)
  const secondsLeft = useCountdown(booking.data?.holdExpiresAt)

  const [paymentToken, setPaymentToken] = useState<string>(TEST_CARDS[0].token)
  const [cardDeclined, setCardDeclined] = useState(false)
  // One key per payment attempt. Retrying after a network error reuses it, so the server can never
  // charge twice. After a decline the server remembers the failed attempt, so we need a fresh key.
  const [idempotencyKey, setIdempotencyKey] = useState(newIdempotencyKey)

  if (booking.isPending) return <Loading label="Loading your booking" />
  if (booking.error) return <ErrorNotice error={booking.error} />

  const { status, seatLabels, totalAmount, eventId } = booking.data
  const seats = seatLabels.join(', ')

  if (status === 'CONFIRMED') {
    return (
      <Outcome title="You're booked" tone="ok">
        <p>
          Seats {seats} are confirmed. We'll email your confirmation shortly.
        </p>
        <Link to="/bookings" className="mt-6 inline-block rounded-lg bg-ink px-4 py-2 font-semibold text-paper">
          View my bookings
        </Link>
      </Outcome>
    )
  }

  const holdOver = status === 'EXPIRED' || secondsLeft === 0
  if (status === 'CANCELLED' || holdOver) {
    return (
      <Outcome title={status === 'CANCELLED' ? 'Booking cancelled' : 'Your hold ran out'} tone="alert">
        <p>The seats {seats} were released. You can pick seats again.</p>
        <Link to={`/events/${eventId}`} className="mt-6 inline-block rounded-lg bg-ink px-4 py-2 font-semibold text-paper">
          Back to the seat map
        </Link>
      </Outcome>
    )
  }

  function submitPayment(event: React.FormEvent) {
    event.preventDefault()
    setCardDeclined(false)
    pay.mutate(
      { paymentToken, idempotencyKey },
      {
        onSuccess: (payment) => {
          if (payment.status === 'FAILED') {
            setCardDeclined(true)
            setIdempotencyKey(newIdempotencyKey())
          }
        },
      },
    )
  }

  return (
    <div className="max-w-xl">
      <h1 className="text-3xl font-extrabold sm:text-4xl">Complete your payment</h1>
      <p className="mt-3 text-lg" role="timer" aria-live="off">
        Seats held for <strong className={secondsLeft < 60 ? 'text-alert' : ''}>{formatCountdown(secondsLeft)}</strong>
      </p>

      <section className="mt-6 rounded-xl bg-card p-5">
        <h2 className="text-lg font-extrabold">Seats {seats}</h2>
        <p className="mt-2 flex justify-between border-t border-line pt-3 font-semibold">
          <span>Total</span>
          <span>{formatPrice(totalAmount)}</span>
        </p>
      </section>

      <form onSubmit={submitPayment} className="mt-6 rounded-xl bg-card p-5">
        <fieldset>
          <legend className="text-lg font-extrabold">Test card</legend>
          <p className="mt-1 text-sm text-ink-soft">No real payment is made. Pick a card to try each outcome.</p>
          <div className="mt-3 flex flex-col gap-2">
            {TEST_CARDS.map((card) => (
              <label
                key={card.token}
                className={`flex cursor-pointer items-start gap-3 rounded-lg border-2 p-3 ${
                  paymentToken === card.token ? 'border-ink' : 'border-line'
                }`}
              >
                <input
                  type="radio"
                  name="card"
                  value={card.token}
                  checked={paymentToken === card.token}
                  onChange={() => setPaymentToken(card.token)}
                  className="mt-1"
                />
                <span>
                  <span className="block font-medium">{card.label}</span>
                  <span className="block text-sm text-ink-soft">{card.hint}</span>
                </span>
              </label>
            ))}
          </div>
        </fieldset>

        {cardDeclined && (
          <p role="alert" className="mt-4 rounded-lg border-2 border-alert px-4 py-3 text-alert">
            Your card was declined. Choose another card and try again. Your seats are still held.
          </p>
        )}
        {pay.error && (
          <div className="mt-4">
            <ErrorNotice error={pay.error} />
          </div>
        )}

        <div className="mt-5 flex flex-wrap gap-3">
          <button
            type="submit"
            disabled={pay.isPending}
            className="rounded-lg bg-ink px-5 py-3 font-semibold text-paper hover:bg-seat-free disabled:opacity-60"
          >
            {pay.isPending ? 'Paying…' : `Pay ${formatPrice(totalAmount)}`}
          </button>
          <button
            type="button"
            disabled={cancel.isPending || pay.isPending}
            onClick={() => cancel.mutate()}
            className="rounded-lg border-2 border-line px-5 py-3 font-semibold text-ink-soft hover:border-ink hover:text-ink"
          >
            Release seats
          </button>
        </div>
        {cancel.error && (
          <div className="mt-4">
            <ErrorNotice error={cancel.error} />
          </div>
        )}
      </form>
    </div>
  )
}

function Outcome({ title, tone, children }: { title: string; tone: 'ok' | 'alert'; children: React.ReactNode }) {
  return (
    <div className="max-w-xl py-6">
      <h1 className={`text-3xl font-extrabold sm:text-4xl ${tone === 'ok' ? 'text-ok' : 'text-alert'}`}>{title}</h1>
      <div className="mt-3 text-lg">{children}</div>
    </div>
  )
}
