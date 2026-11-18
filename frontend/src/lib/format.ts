const rupees = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 2,
  minimumFractionDigits: 0,
})

export const formatPrice = (amount: number): string => rupees.format(amount)

export const formatEventDate = (iso: string): string =>
  new Date(iso).toLocaleString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    hour: 'numeric',
    minute: '2-digit',
  })

/** 4:07 style countdown; never negative. */
export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, totalSeconds)
  const minutes = Math.floor(safe / 60)
  const seconds = safe % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

/** Seconds left until an ISO timestamp, measured against the browser clock. */
export const secondsUntil = (iso: string, now: number = Date.now()): number =>
  Math.ceil((new Date(iso).getTime() - now) / 1000)
