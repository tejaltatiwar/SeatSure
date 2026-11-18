import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, clearSession, hasSession, onSessionExpired, saveSession } from '../api/client'
import type { AuthResponse, User } from '../api/types'

const USER_KEY = 'seatsure.user'

interface AuthContextValue {
  user: User | null
  login: (email: string, password: string) => Promise<void>
  register: (fullName: string, email: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readStoredUser(): User | null {
  if (!hasSession()) return null
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as User) : null
  } catch {
    return null
  }
}

function storeUser(user: User | null): void {
  try {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
    else localStorage.removeItem(USER_KEY)
  } catch {
    // Ignore: storage may be unavailable.
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readStoredUser)

  const logout = useCallback(() => {
    clearSession()
    storeUser(null)
    setUser(null)
  }, [])

  // If a token refresh fails anywhere in the app, drop the user back to the signed-out state.
  useEffect(() => onSessionExpired(logout), [logout])

  const adopt = useCallback((auth: AuthResponse) => {
    saveSession(auth)
    storeUser(auth.user)
    setUser(auth.user)
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      logout,
      login: async (email, password) => adopt(await api.login(email, password)),
      register: async (fullName, email, password) => adopt(await api.register(fullName, email, password)),
    }),
    [user, logout, adopt],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
