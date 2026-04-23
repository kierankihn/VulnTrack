const BASE = '/api/v1'

let _token: string | null = null

export function setAccessToken(token: string | null) {
  _token = token
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json', ...(options?.headers as Record<string, string>) }
  const token = _token ?? localStorage.getItem('accessToken')
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(BASE + path, { ...options, headers })
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(err.message || err.error || 'Request failed')
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

// ── Types ──────────────────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface CveDto {
  id: number
  cveId: string
  description: string
  severity: string
  cvssV3Score: number | null
  cvssV2Score: number | null
  cvssV3Vector: string | null
  publishedDate: string
  lastModifiedDate: string
  affectedProducts: string | null
  references: string | null
  syncedAt: string
  ticketCount: number
}

export interface TicketDto {
  id: number
  title: string
  description: string
  status: string
  priority: string
  source: string
  assetId: number | null
  assetName: string | null
  assigneeId: number | null
  assigneeName: string | null
  reporterId: number | null
  reporterName: string | null
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
  scanInfo: string | null
  comment: string | null
  cves: CveDto[]
  statusHistory: string[]
  allowedTransitions: string[]
}

export interface AssetDto {
  id: number
  name: string
  description: string | null
  repoUrl: string | null
  projectName: string | null
  devGroupId: number | null
  devGroupName: string | null
  createdAt: string
  openTicketCount: number
}

export interface DevGroupDto {
  id: number
  name: string
  description: string | null
  createdAt: string
  memberCount: number
  assetCount: number
  leaderId: number | null
  leaderName: string | null
  members?: UserDto[]
}

export interface UserDto {
  id: number
  username: string
  email: string
  fullName: string
  role: string
  active: boolean
  devGroupId: number | null
  devGroupName: string | null
  createdAt: string
}

export interface TicketStats {
  open: number
  inProgress: number
  inReview: number
  resolved: number
  closed: number
  wontFix: number
  critical: number
  high: number
}

export interface CveStats {
  total: number
  critical: number
  high: number
  medium: number
  low: number
}

// ── CVE API ────────────────────────────────────────────────────────────────

export const cveApi = {
  search: (params: Record<string, unknown> = {}) => {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => v != null && q.set(k, String(v)))
    return request<PageResponse<CveDto>>(`/cves?${q}`)
  },
  getById: (id: number) => request<CveDto>(`/cves/${id}`),
  getByCveId: (cveId: string) => request<CveDto>(`/cves/cve/${cveId}`),
  getStats: () => request<CveStats>('/cves/stats'),
  getTickets: (id: number) => request<TicketDto[]>(`/cves/${id}/tickets`),
  triggerSync: () => request<{ added: number; updated: number; error?: string }>('/cves/sync', { method: 'POST' }),
}

// ── Ticket API ─────────────────────────────────────────────────────────────

export const ticketApi = {
  search: (params: Record<string, unknown> = {}) => {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => v != null && q.set(k, String(v)))
    return request<PageResponse<TicketDto>>(`/tickets?${q}`)
  },
  getById: (id: number) => request<TicketDto>(`/tickets/${id}`),
  getStats: () => request<TicketStats>('/tickets/stats'),
  create: (data: object) => request<TicketDto>('/tickets', { method: 'POST', body: JSON.stringify(data) }),
  transition: (id: number, data: { targetStatus: string; comment?: string }) =>
    request<TicketDto>(`/tickets/${id}/transition`, { method: 'POST', body: JSON.stringify(data) }),
  updateAssignee: (id: number, assigneeId: number) =>
    request<TicketDto>(`/tickets/${id}/assignee`, { method: 'PUT', body: JSON.stringify({ assigneeId }) }),
  addCve: (id: number, cveId: string) =>
    request<TicketDto>(`/tickets/${id}/cves/${cveId}`, { method: 'POST' }),
  removeCve: (id: number, cveId: string) =>
    request<TicketDto>(`/tickets/${id}/cves/${cveId}`, { method: 'DELETE' }),
}

// ── Asset API ──────────────────────────────────────────────────────────────

export const assetApi = {
  search: (params: Record<string, unknown> = {}) => {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => v != null && q.set(k, String(v)))
    return request<PageResponse<AssetDto>>(`/assets?${q}`)
  },
  getById: (id: number) => request<AssetDto>(`/assets/${id}`),
  create: (data: object) => request<AssetDto>('/assets', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: object) =>
    request<AssetDto>(`/assets/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => request<void>(`/assets/${id}`, { method: 'DELETE' }),
}

// ── DevGroup API ───────────────────────────────────────────────────────────

export const devGroupApi = {
  findAll: () => request<DevGroupDto[]>('/dev-groups'),
  getById: (id: number) => request<DevGroupDto>(`/dev-groups/${id}`),
  create: (data: object) => request<DevGroupDto>('/dev-groups', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: object) =>
    request<DevGroupDto>(`/dev-groups/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => request<void>(`/dev-groups/${id}`, { method: 'DELETE' }),
}

// ── User API ───────────────────────────────────────────────────────────────

export const userApi = {
  search: (params: Record<string, unknown> = {}) => {
    const q = new URLSearchParams()
    Object.entries(params).forEach(([k, v]) => v != null && q.set(k, String(v)))
    return request<PageResponse<UserDto>>(`/users?${q}`)
  },
  assignable: () => request<PageResponse<UserDto>>('/users/assignable'),
  getById: (id: number) => request<UserDto>(`/users/${id}`),
  create: (data: object) => request<UserDto>('/users', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: object) =>
    request<UserDto>(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  setActive: (id: number, active: boolean) =>
    request<UserDto>(`/users/${id}/active`, { method: 'PATCH', body: JSON.stringify({ active }) }),
  resetPassword: (id: number, newPassword: string) =>
    request<void>(`/users/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ newPassword }) }),
}

export const authApi = {
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
}

export interface NvdKeyStatus {
  configured: boolean
  maskedValue: string
}

export const settingsApi = {
  getNvdKey: () => request<NvdKeyStatus>('/settings/nvd-api-key'),
  setNvdKey: (value: string) =>
    request<void>('/settings/nvd-api-key', { method: 'PUT', body: JSON.stringify({ value }) }),
}
