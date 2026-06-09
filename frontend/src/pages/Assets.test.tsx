import { render, screen, waitFor } from '@testing-library/react'
import Assets from './Assets'
import { AuthProvider } from '@/lib/auth'

const mocks = vi.hoisted(() => ({
  assetSearch: vi.fn(),
  devGroupFindAll: vi.fn(),
}))

vi.mock('@/lib/api', () => ({
  AUTH_UNAUTHORIZED_EVENT: 'vulntrack:unauthorized',
  clearStoredAuth: vi.fn(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('authUser')
  }),
  setAccessToken: vi.fn(),
  assetApi: {
    search: mocks.assetSearch,
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  devGroupApi: {
    findAll: mocks.devGroupFindAll,
  },
}))

describe('Assets', () => {
  beforeEach(() => {
    localStorage.setItem('accessToken', 'token')
    localStorage.setItem('authUser', JSON.stringify({
      id: 1,
      username: 'admin',
      fullName: 'Admin',
      role: 'ADMIN',
      devGroupId: null,
      devGroupName: null,
      active: true,
    }))
    mocks.assetSearch.mockResolvedValue({
      content: [
        {
          id: 1,
          name: 'gateway',
          description: null,
          repoUrl: 'not-a-valid-url',
          projectName: 'gateway',
          devGroupId: null,
          devGroupName: null,
          createdAt: '2026-04-21T10:00:00',
          openTicketCount: 0,
        },
      ],
    })
    mocks.devGroupFindAll.mockResolvedValue([])
  })

  afterEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('renders malformed repo urls without crashing', async () => {
    render(
      <AuthProvider>
        <Assets />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('not-a-valid-url')).toBeInTheDocument()
    })
  })
})
