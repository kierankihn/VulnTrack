import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import TicketDetail from './TicketDetail'
import { AuthProvider } from '@/lib/auth'

const mocks = vi.hoisted(() => ({
  ticketGetById: vi.fn(),
  userAssignable: vi.fn(),
}))

vi.mock('@/lib/api', () => ({
  AUTH_UNAUTHORIZED_EVENT: 'vulntrack:unauthorized',
  clearStoredAuth: vi.fn(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('authUser')
  }),
  setAccessToken: vi.fn(),
  ticketApi: {
    getById: mocks.ticketGetById,
    transition: vi.fn(),
    addCve: vi.fn(),
    removeCve: vi.fn(),
    updateAssignee: vi.fn(),
  },
  userApi: {
    assignable: mocks.userAssignable,
  },
  cveApi: {},
}))

describe('TicketDetail', () => {
  beforeEach(() => {
    localStorage.setItem('accessToken', 'token')
    localStorage.setItem('authUser', JSON.stringify({
      id: 1,
      username: 'lead',
      fullName: 'Lead',
      role: 'GROUP_LEAD',
      devGroupId: 1,
      devGroupName: 'Blue Team',
      active: true,
    }))
    mocks.ticketGetById.mockResolvedValue({
      id: 1,
      title: 'Fix gateway vuln',
      description: 'details',
      status: 'OPEN',
      priority: 'HIGH',
      source: 'AUTO_SCAN',
      assetId: 10,
      assetName: 'gateway',
      assigneeId: null,
      assigneeName: null,
      reporterId: null,
      reporterName: null,
      createdAt: '2026-04-21T10:00:00',
      updatedAt: '2026-04-21T11:00:00',
      resolvedAt: null,
      scanInfo: 'not-json',
      comment: null,
      cves: [],
      statusHistory: [],
      allowedTransitions: [],
    })
    mocks.userAssignable.mockResolvedValue({ content: [] })
  })

  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders malformed scan info without crashing', async () => {
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/tickets/1']}>
          <Routes>
            <Route path="/tickets/:id" element={<TicketDetail />} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('not-json')).toBeInTheDocument()
    })
  })
})
