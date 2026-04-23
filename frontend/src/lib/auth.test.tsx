import { canAssignTicket, canCreateTicket, canManageAssets, canSyncCves, hasAccess } from './auth'

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
})
