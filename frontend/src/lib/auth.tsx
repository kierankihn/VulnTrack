import { createContext, useContext, useState, useEffect } from 'react'
import { setAccessToken } from './api'

export interface AuthUser {
  id: number
  username: string
  fullName: string
  role: string
  devGroupId: number | null
  devGroupName: string | null
  active: boolean
}

interface AuthContextType {
  user: AuthUser | null
  loading: boolean
  login: (usernameOrEmail: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export type Role = 'ADMIN' | 'GROUP_LEAD' | 'DEVELOPER' | 'TESTER'

export const PAGE_ACCESS: Record<string, Role[]> = {
  dashboard:   ['ADMIN', 'GROUP_LEAD', 'DEVELOPER', 'TESTER'],
  cves:        ['ADMIN', 'GROUP_LEAD', 'DEVELOPER', 'TESTER'],
  tickets:     ['ADMIN', 'GROUP_LEAD', 'DEVELOPER', 'TESTER'],
  ticketCreate:['ADMIN', 'TESTER'],
  assets:      ['ADMIN', 'GROUP_LEAD'],
  devGroups:   ['ADMIN'],
  users:       ['ADMIN'],
  settings:    ['ADMIN'],
}

export function hasAccess(role: string | undefined | null, page: keyof typeof PAGE_ACCESS): boolean {
  if (!role) return false
  return (PAGE_ACCESS[page] as string[]).includes(role)
}

export function canCreateTicket(role: string | undefined | null): boolean {
  return role === 'ADMIN' || role === 'TESTER'
}

export function canAssignTicket(role: string | undefined | null): boolean {
  return role === 'ADMIN' || role === 'GROUP_LEAD'
}

export function canManageAssets(role: string | undefined | null): boolean {
  return role === 'ADMIN'
}

export function canSyncCves(role: string | undefined | null): boolean {
  return role === 'ADMIN'
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    const stored = localStorage.getItem('authUser')
    if (token && stored) {
      try {
        setUser(JSON.parse(stored))
        setAccessToken(token)
      } catch {
        localStorage.removeItem('authUser')
        localStorage.removeItem('accessToken')
      }
    }
    setLoading(false)
  }, [])

  const login = async (usernameOrEmail: string, password: string) => {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ usernameOrEmail, password }),
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || '用户名或密码错误')
    }
    const data = await res.json()
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('authUser', JSON.stringify(data.user))
    setAccessToken(data.accessToken)
    setUser(data.user)
  }

  const logout = async () => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      }).catch(() => {})
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('authUser')
    setAccessToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
