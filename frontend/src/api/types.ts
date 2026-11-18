// Mirrors the JSON contracts of the SeatSure API (see backend DTO records).

export type Role = 'CUSTOMER' | 'ADMIN'

export interface User {
  id: number
  fullName: string
  email: string
  role: Role
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  accessTokenExpiresInSeconds: number
  user: User
}

export interface EventSummary {
  id: number
  title: string
  description: string | null
  venue: string
  city: string
  startsAt: string
  totalSeats: number
  startingPrice: number
}

export interface SeatAvailability {
  seatId: number
  rowLabel: string
  seatNumber: number
  price: number
  available: boolean
}

export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED'

export interface Booking {
  id: number
  eventId: number
  status: BookingStatus
  totalAmount: number
  holdExpiresAt: string
  createdAt: string
  seatLabels: string[]
}

export type PaymentStatus = 'SUCCEEDED' | 'FAILED'

export interface Payment {
  paymentId: number
  bookingId: number
  status: PaymentStatus
  amount: number
  gatewayReference: string | null
}

export interface ApiErrorBody {
  status: number
  code: string
  message: string
  fieldErrors?: Record<string, string>
}
