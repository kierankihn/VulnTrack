import { act, render, screen, waitFor } from '@testing-library/react'
import { AUTH_UNAUTHORIZED_EVENT } from './api'
import { AuthProvider, canAssignTicket, canCreateTicket, canManageAssets, canSyncCves, hasAccess, useAuth } from './auth'

function AuthStateProbe() {
  const { user } = useAuth()
  return <div>{user ? user.fullName : 'anonymous'}</div>
}

describe('auth helpers', () => {
  it('limits ticket creation to admins and testers', () => {
    expect(canCreateTicket('ADMIN')).toBe(true)
    expect(canCreateTicket('TESTER')).toBe(true)
    expect(canCreateTicket('GROUP_LEAD')).toBe(false)
    expect(canCreateTicket('DEVELOPER')).toBe(false)
  })

  it('limits assignment to admins and group leads', () => {
    expect(canAssignTicket('ADMIN')).toBe(true)
    expect(canAssignTicket('GROUP_LEAD')).toBe(true)
    expect(canAssignTicket('TESTER')).toBe(false)
    expect(canAssignTicket('DEVELOPER')).toBe(false)
  })

  it('keeps admin-only pages restricted', () => {
    expect(hasAccess('ADMIN', 'users')).toBe(true)
    expect(hasAccess('GROUP_LEAD', 'users')).toBe(false)
    expect(hasAccess('DEVELOPER', 'settings')).toBe(false)
  })

  it('keeps asset management and cve sync admin-only', () => {
    expect(canManageAssets('ADMIN')).toBe(true)
    expect(canManageAssets('GROUP_LEAD')).toBe(false)
    expect(canSyncCves('ADMIN')).toBe(true)
    expect(canSyncCves('TESTER')).toBe(false)
  })

  it('clears current user when an unauthorized event is received', async () => {
    localStorage.setItem('accessToken', 'expired-token')
    localStorage.setItem('authUser', JSON.stringify({
      id: 1,
      username: 'admin',
      fullName: 'Admin User',
      role: 'ADMIN',
      devGroupId: null,
      devGroupName: null,
      active: true,
    }))

    render(
      <AuthProvider>
        <AuthStateProbe />
      </AuthProvider>
    )

    expect(await screen.findByText('Admin User')).toBeInTheDocument()

    act(() => {
      window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT))
    })

    await waitFor(() => expect(screen.getByText('anonymous')).toBeInTheDocument())
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('authUser')).toBeNull()
  })
})
