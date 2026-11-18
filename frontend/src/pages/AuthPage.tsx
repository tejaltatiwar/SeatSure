import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { login, register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const returnTo = (location.state as { from?: string } | null)?.from ?? '/'

  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<ApiError | Error | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const isRegister = mode === 'register'
  const fieldErrors = error instanceof ApiError ? error.fieldErrors : {}

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)
    try {
      if (isRegister) await register(fullName, email, password)
      else await login(email, password)
      navigate(returnTo, { replace: true })
    } catch (caught) {
      setError(caught instanceof Error ? caught : new Error('Unexpected error'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-sm">
      <h1 className="text-3xl font-extrabold">{isRegister ? 'Create your account' : 'Sign in'}</h1>
      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4 rounded-xl bg-card p-5">
        {isRegister && (
          <Field label="Full name" error={fieldErrors.fullName}>
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} required autoComplete="name" className={inputClass} />
          </Field>
        )}
        <Field label="Email" error={fieldErrors.email}>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" className={inputClass} />
        </Field>
        <Field label="Password" error={fieldErrors.password} hint={isRegister ? 'At least 8 characters' : undefined}>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={isRegister ? 8 : undefined}
            autoComplete={isRegister ? 'new-password' : 'current-password'}
            className={inputClass}
          />
        </Field>

        {error && Object.keys(fieldErrors).length === 0 && (
          <p role="alert" className="rounded-lg border-2 border-alert px-3 py-2 text-alert">
            {error.message}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          className="rounded-lg bg-ink px-4 py-3 font-semibold text-paper hover:bg-seat-free disabled:opacity-60"
        >
          {submitting ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'}
        </button>
      </form>

      <p className="mt-4 text-ink-soft">
        {isRegister ? 'Already have an account? ' : 'New to SeatSure? '}
        <Link to={isRegister ? '/login' : '/register'} state={location.state} className="font-semibold text-ink underline">
          {isRegister ? 'Sign in' : 'Create an account'}
        </Link>
      </p>
    </div>
  )
}

const inputClass = 'w-full rounded-lg border-2 border-line bg-paper px-3 py-2 focus:border-ink focus:outline-none'

function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string
  error?: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <label className="flex flex-col gap-1 font-medium">
      {label}
      {children}
      {hint && !error && <span className="text-sm font-normal text-ink-soft">{hint}</span>}
      {error && <span className="text-sm font-normal text-alert">{error}</span>}
    </label>
  )
}
