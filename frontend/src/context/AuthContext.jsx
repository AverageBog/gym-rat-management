import { createContext, useContext, useState, useEffect, useRef } from 'react'

const SESSION_TTL_MS = 3 * 60 * 60 * 1000 // 3 hours

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('auth')
      if (!stored) return null
      const parsed = JSON.parse(stored)
      if (parsed.expiresAt < Date.now()) {
        localStorage.removeItem('auth')
        return null
      }
      return parsed
    } catch {
      return null
    }
  })

  const logoutTimerRef = useRef(null)

  function scheduleAutoLogout(expiresAt) {
    clearTimeout(logoutTimerRef.current)
    const remaining = expiresAt - Date.now()
    if (remaining > 0) {
      logoutTimerRef.current = setTimeout(logout, remaining)
    } else {
      logout()
    }
  }

  useEffect(() => {
    if (user) scheduleAutoLogout(user.expiresAt)
    return () => clearTimeout(logoutTimerRef.current)
  }, [])

  function login(userData) {
    const expiresAt = Date.now() + SESSION_TTL_MS
    const data = { ...userData, expiresAt }
    localStorage.setItem('auth', JSON.stringify(data))
    setUser(data)
    scheduleAutoLogout(expiresAt)
  }

  function logout() {
    clearTimeout(logoutTimerRef.current)
    localStorage.removeItem('auth')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
