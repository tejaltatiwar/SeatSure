import { useEffect, useState } from 'react'
import { secondsUntil } from './format'

/** Whole seconds remaining until `isoDeadline`, ticking once a second. Stops at zero. */
export function useCountdown(isoDeadline: string | undefined): number {
  const [secondsLeft, setSecondsLeft] = useState(() => (isoDeadline ? secondsUntil(isoDeadline) : 0))

  useEffect(() => {
    if (!isoDeadline) return
    setSecondsLeft(secondsUntil(isoDeadline))
    const timer = window.setInterval(() => {
      const remaining = secondsUntil(isoDeadline)
      setSecondsLeft(remaining)
      if (remaining <= 0) window.clearInterval(timer)
    }, 1000)
    return () => window.clearInterval(timer)
  }, [isoDeadline])

  return Math.max(0, secondsLeft)
}
