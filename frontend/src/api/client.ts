import type {
  ApiErrorBody,
  AuthResponse,
  Booking,
  EventSummary,
  Payment,
  SeatAvailability,
} from './types'

const API_BASE = '/api/v1'
const STORAGE_KEY = 'seatsure.session'

interface Session {
  accessToken: string
  refreshToken: string
}

/** Raised for every non-2xx answer; `code` is the stable machine-readable error code from the API. */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>
  readonly body: unknown

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}, body?: unknown) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
    this.body = body
  }
}

// ---- session storage -------------------------------------------------------------------------
// Tokens live in localStorage: simple and survives reloads. Trade-off: readable by any script on
// the page, so a real deployment should add a strict CSP (or move the refresh token to an
// httpOnly cookie).

function readSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

function writeSession(session: Session | null): void {
  try {
    if (session) localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Storage can be blocked (private mode); the app then simply forgets the login on reload.
  }
}

let session: Session | null = readSession()
const sessionExpiredListeners = new Set<() => void>()

export function saveSession(auth: AuthResponse): void {
  session = { accessToken: auth.accessToken, refreshToken: auth.refreshToken }
  writeSession(session)
}

export function clearSession(): void {
  session = null
  writeSession(null)
}

export function hasSession(): boolean {
  return session !== null
}

/** Lets the auth context log the user out when a refresh fails. Returns an unsubscribe function. */
export function onSessionExpired(listener: () => void): () => void {
  sessionExpiredListeners.add(listener)
  return () => sessionExpiredListeners.delete(listener)
}

// ---- request core ----------------------------------------------------------------------------

let refreshInFlight: Promise<boolean> | null = null

/** One refresh at a time: parallel requests that all hit 401 share a single refresh call. */
function refreshSession(): Promise<boolean> {
  if (!session) return Promise.resolve(false)
  refreshInFlight ??= (async () => {
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: session?.refreshToken }),
      })
      if (!response.ok) return false
      saveSession((await response.json()) as AuthResponse)
      return true
    } catch {
      return false
    } finally {
      refreshInFlight = null
    }
  })()
  return refreshInFlight
}

interface RequestOptions {
  method?: 'GET' | 'POST'
  body?: unknown
  headers?: Record<string, string>
}

async function request<T>(path: string, options: RequestOptions = {}, allowRefresh = true): Promise<T> {
  const headers: Record<string, string> = { ...options.headers }
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  if (session) headers.Authorization = `Bearer ${session.accessToken}`

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  })

  if (response.status === 401 && allowRefresh && session && !path.startsWith('/auth/')) {
    if (await refreshSession()) return request<T>(path, options, false)
    clearSession()
    sessionExpiredListeners.forEach((listener) => listener())
  }

  if (!response.ok) throw await toApiError(response)
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as Partial<ApiErrorBody>
    return new ApiError(
      response.status,
      body.code ?? 'UNKNOWN',
      body.message ?? 'Something went wrong. Please try again.',
      body.fieldErrors ?? {},
      body,
    )
  } catch {
    return new ApiError(response.status, 'UNKNOWN', 'Something went wrong. Please try again.')
  }
}

// ---- endpoints -------------------------------------------------------------------------------

export const api = {
  register: (fullName: string, email: string, password: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: { fullName, email, password } }),

  login: (email: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),

  listEvents: () => request<EventSummary[]>('/events'),
  getEvent: (eventId: number) => request<EventSummary>(`/events/${eventId}`),
  getSeats: (eventId: number) => request<SeatAvailability[]>(`/events/${eventId}/seats`),

  createBooking: (eventId: number, seatIds: number[]) =>
    request<Booking>('/bookings', { method: 'POST', body: { eventId, seatIds } }),
  listBookings: () => request<Booking[]>('/bookings'),
  getBooking: (bookingId: number) => request<Booking>(`/bookings/${bookingId}`),
  cancelBooking: (bookingId: number) => request<Booking>(`/bookings/${bookingId}/cancel`, { method: 'POST' }),

  /** A declined card comes back as HTTP 402 with a Payment body (status FAILED); we return it as data. */
  async pay(bookingId: number, paymentToken: string, idempotencyKey: string): Promise<Payment> {
    try {
      return await request<Payment>(`/bookings/${bookingId}/payments`, {
        method: 'POST',
        body: { paymentToken },
        headers: { 'Idempotency-Key': idempotencyKey },
      })
    } catch (error) {
      if (error instanceof ApiError && error.status === 402) return error.body as Payment
      throw error
    }
  },
}
