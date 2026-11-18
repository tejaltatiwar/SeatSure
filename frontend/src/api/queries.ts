import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'

export const queryKeys = {
  events: ['events'] as const,
  event: (eventId: number) => ['events', eventId] as const,
  seats: (eventId: number) => ['events', eventId, 'seats'] as const,
  bookings: ['bookings'] as const,
  booking: (bookingId: number) => ['bookings', bookingId] as const,
}

export const useEvents = () => useQuery({ queryKey: queryKeys.events, queryFn: api.listEvents })

export const useEvent = (eventId: number) =>
  useQuery({ queryKey: queryKeys.event(eventId), queryFn: () => api.getEvent(eventId) })

/** Seat map refreshes every 15s so people see seats disappear while they browse. */
export const useSeats = (eventId: number) =>
  useQuery({
    queryKey: queryKeys.seats(eventId),
    queryFn: () => api.getSeats(eventId),
    refetchInterval: 15_000,
  })

export const useBookings = () => useQuery({ queryKey: queryKeys.bookings, queryFn: api.listBookings })

export const useBooking = (bookingId: number) =>
  useQuery({ queryKey: queryKeys.booking(bookingId), queryFn: () => api.getBooking(bookingId) })

export function useCreateBooking(eventId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (seatIds: number[]) => api.createBooking(eventId, seatIds),
    // Win or lose, the seat map is stale now: either our seats are held or someone beat us to them.
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.seats(eventId) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.bookings }),
  })
}

export function useCancelBooking(bookingId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.cancelBooking(bookingId),
    onSuccess: (booking) => {
      queryClient.setQueryData(queryKeys.booking(bookingId), booking)
      queryClient.invalidateQueries({ queryKey: queryKeys.bookings })
      queryClient.invalidateQueries({ queryKey: queryKeys.seats(booking.eventId) })
    },
  })
}

export function usePay(bookingId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (args: { paymentToken: string; idempotencyKey: string }) =>
      api.pay(bookingId, args.paymentToken, args.idempotencyKey),
    onSuccess: (payment) => {
      if (payment.status === 'SUCCEEDED') {
        queryClient.invalidateQueries({ queryKey: queryKeys.booking(bookingId) })
        queryClient.invalidateQueries({ queryKey: queryKeys.bookings })
      }
    },
  })
}
