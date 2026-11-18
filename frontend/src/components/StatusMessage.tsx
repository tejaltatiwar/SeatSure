import { ApiError } from '../api/client'

export function Loading({ label = 'Loading' }: { label?: string }) {
  return (
    <p role="status" className="py-10 text-ink-soft">
      {label}…
    </p>
  )
}

/** Shows what went wrong in the API's own words; any other error gets a generic retry hint. */
export function ErrorNotice({ error }: { error: unknown }) {
  const message =
    error instanceof ApiError ? error.message : 'We could not reach the server. Check your connection and try again.'
  return (
    <p role="alert" className="rounded-lg border-2 border-alert bg-card px-4 py-3 text-alert">
      {message}
    </p>
  )
}

export function EmptyState({ title, hint }: { title: string; hint: string }) {
  return (
    <div className="py-12">
      <h2 className="text-xl font-semibold">{title}</h2>
      <p className="mt-1 text-ink-soft">{hint}</p>
    </div>
  )
}
