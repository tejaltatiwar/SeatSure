import { Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthPage } from './pages/AuthPage'
import { BookingsPage } from './pages/BookingsPage'
import { CheckoutPage } from './pages/CheckoutPage'
import { EventPage } from './pages/EventPage'
import { EventsPage } from './pages/EventsPage'

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<EventsPage />} />
        <Route path="events/:eventId" element={<EventPage />} />
        <Route path="login" element={<AuthPage mode="login" />} />
        <Route path="register" element={<AuthPage mode="register" />} />
        <Route element={<ProtectedRoute />}>
          <Route path="checkout/:bookingId" element={<CheckoutPage />} />
          <Route path="bookings" element={<BookingsPage />} />
        </Route>
        <Route path="*" element={<p className="py-10">That page does not exist.</p>} />
      </Route>
    </Routes>
  )
}
