import { useMemo } from 'react'
import type { SeatAvailability } from '../api/types'

interface SeatMapProps {
  seats: SeatAvailability[]
  selectedSeatIds: ReadonlySet<number>
  /** When true, unselected seats cannot be added (per-booking limit reached). */
  selectionFull: boolean
  onToggleSeat: (seat: SeatAvailability) => void
}

export function SeatMap({ seats, selectedSeatIds, selectionFull, onToggleSeat }: SeatMapProps) {
  const rows = useMemo(() => {
    const byRow = new Map<string, SeatAvailability[]>()
    for (const seat of seats) {
      byRow.set(seat.rowLabel, [...(byRow.get(seat.rowLabel) ?? []), seat])
    }
    return [...byRow.entries()]
  }, [seats])

  return (
    <div className="rounded-xl bg-card p-4 sm:p-6">
      {/* Only the seats scroll sideways on narrow screens; the legend below stays in view. */}
      <div className="overflow-x-auto p-1">
        <div className="mx-auto w-max min-w-full">
          <div className="mb-6 rounded-md bg-ink py-2 text-center text-sm font-semibold text-paper">Stage</div>
          <div role="group" aria-label="Seat map" className="flex flex-col items-center gap-2">
            {rows.map(([rowLabel, rowSeats]) => (
              <div key={rowLabel} className="flex items-center gap-2">
                <span aria-hidden className="w-5 text-right text-sm font-semibold text-ink-soft">
                  {rowLabel}
                </span>
                <div className="flex gap-1.5">
                  {rowSeats.map((seat) => (
                    <SeatButton
                      key={seat.seatId}
                      seat={seat}
                      picked={selectedSeatIds.has(seat.seatId)}
                      selectionFull={selectionFull}
                      onToggle={onToggleSeat}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <ul className="mt-6 flex flex-wrap justify-center gap-x-5 gap-y-2 text-sm text-ink-soft">
        <Legend state="free" label="Available" />
        <Legend state="picked" label="Your selection" />
        <Legend state="taken" label="Taken" />
      </ul>
    </div>
  )
}

interface SeatButtonProps {
  seat: SeatAvailability
  picked: boolean
  selectionFull: boolean
  onToggle: (seat: SeatAvailability) => void
}

function SeatButton({ seat, picked, selectionFull, onToggle }: SeatButtonProps) {
  const state = picked ? 'picked' : seat.available ? 'free' : 'taken'
  const blocked = !seat.available || (!picked && selectionFull)
  const stateLabel = state === 'taken' ? 'taken' : picked ? 'selected' : 'available'

  return (
    <button
      type="button"
      className="seat"
      data-state={state}
      disabled={blocked}
      aria-pressed={picked}
      aria-label={`Row ${seat.rowLabel}, seat ${seat.seatNumber}, ${stateLabel}`}
      onClick={() => onToggle(seat)}
    >
      {seat.seatNumber}
    </button>
  )
}

function Legend({ state, label }: { state: 'free' | 'picked' | 'taken'; label: string }) {
  return (
    <li className="flex items-center gap-2">
      <span aria-hidden className="seat !h-5 !w-5" data-state={state} />
      {label}
    </li>
  )
}
