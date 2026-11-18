import { useMemo, useState } from 'react'
import { useEvents } from '../api/queries'
import { TicketCard } from '../components/TicketCard'
import { EmptyState, ErrorNotice, Loading } from '../components/StatusMessage'

export function EventsPage() {
  const { data: events, isPending, error } = useEvents()
  const [city, setCity] = useState<string | null>(null)

  const cities = useMemo(() => [...new Set((events ?? []).map((event) => event.city))].sort(), [events])
  const visibleEvents = useMemo(
    () => (events ?? []).filter((event) => city === null || event.city === city),
    [events, city],
  )

  if (isPending) return <Loading label="Loading events" />
  if (error) return <ErrorNotice error={error} />

  return (
    <>
      <h1 className="max-w-xl text-4xl font-extrabold sm:text-5xl">Pick a show. Pick your seats.</h1>
      <p className="mt-3 max-w-prose text-ink-soft">
        Seats you choose are held for five minutes while you pay, so nobody can take them from you.
      </p>

      {cities.length > 1 && (
        <div role="group" aria-label="Filter by city" className="mt-8 flex flex-wrap gap-2">
          <CityChip label="All cities" active={city === null} onClick={() => setCity(null)} />
          {cities.map((name) => (
            <CityChip key={name} label={name} active={city === name} onClick={() => setCity(name)} />
          ))}
        </div>
      )}

      {visibleEvents.length === 0 ? (
        <EmptyState title="No upcoming events" hint="New events appear here as soon as they are announced." />
      ) : (
        <div className="mt-6 flex flex-col gap-4">
          {visibleEvents.map((event) => (
            <TicketCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </>
  )
}

function CityChip({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={`rounded-full border-2 px-4 py-1.5 text-sm font-medium ${
        active ? 'border-ink bg-ink text-paper' : 'border-line bg-card text-ink-soft hover:border-ink hover:text-ink'
      }`}
    >
      {label}
    </button>
  )
}
