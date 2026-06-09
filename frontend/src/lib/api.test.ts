import { AUTH_UNAUTHORIZED_EVENT, assetApi, cveApi, setAccessToken, ticketApi } from './api'

describe('api helpers', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    localStorage.clear()
    setAccessToken(null)
    vi.unstubAllGlobals()
    vi.clearAllMocks()
  })

  it('serializes query params and skips null values', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    )

    await cveApi.search({ q: 'openssl', severity: 'HIGH', page: 2, size: null })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/cves?q=openssl&severity=HIGH&page=2',
      expect.objectContaining({
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
  })

  it('returns undefined for 204 responses', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(assetApi.delete(1)).resolves.toBeUndefined()
  })

  it('sends json payloads for writes', async () => {
    fetchMock.mockResolvedValue(
      new Response(JSON.stringify({ id: 1 }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    )

    await ticketApi.create({ title: 'Fix it' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/tickets',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ title: 'Fix it' }),
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    )
  })

  it('falls back to status text when an error response is not json', async () => {
    fetchMock.mockResolvedValue(new Response('boom', { status: 500, statusText: 'Server Error' }))

    await expect(ticketApi.getStats()).rejects.toThrow('Server Error')
  })

  it('clears stored auth and emits an unauthorized event on 401 responses', async () => {
    const listener = vi.fn()
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, listener)
    localStorage.setItem('accessToken', 'expired-token')
    localStorage.setItem('refreshToken', 'refresh-token')
    localStorage.setItem('authUser', JSON.stringify({ username: 'admin' }))
    setAccessToken('expired-token')
    fetchMock.mockResolvedValue(new Response(JSON.stringify({ message: 'Unauthorized' }), { status: 401 }))

    await expect(ticketApi.getStats()).rejects.toThrow('Unauthorized')

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(localStorage.getItem('authUser')).toBeNull()
    expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, listener)
  })
})
