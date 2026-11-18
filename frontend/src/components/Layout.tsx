import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-2 py-1 font-medium ${isActive ? 'bg-ink text-paper' : 'text-ink-soft hover:text-ink'}`

export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="mx-auto flex min-h-screen max-w-5xl flex-col px-4 sm:px-6">
      <header className="flex flex-wrap items-center justify-between gap-3 py-5">
        <Link to="/" className="font-display text-2xl font-extrabold tracking-tight">
          SeatSure
        </Link>
        <nav aria-label="Main" className="flex items-center gap-2 text-sm">
          <NavLink to="/" end className={navLinkClass}>
            Events
          </NavLink>
          {user && (
            <NavLink to="/bookings" className={navLinkClass}>
              My bookings
            </NavLink>
          )}
          {user ? (
            <button
              type="button"
              onClick={() => {
                logout()
                navigate('/')
              }}
              className="rounded-md px-2 py-1 font-medium text-ink-soft hover:text-ink"
            >
              Sign out
            </button>
          ) : (
            <NavLink to="/login" className={navLinkClass}>
              Sign in
            </NavLink>
          )}
        </nav>
      </header>
      <main className="flex-1 pb-16">
        <Outlet />
      </main>
    </div>
  )
}
