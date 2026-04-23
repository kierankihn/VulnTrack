import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { ticketApi, assetApi, userApi, type TicketDto, type PageResponse, type AssetDto, type UserDto } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useAuth, canCreateTicket } from '@/lib/auth'
import { STATUS_COLORS, STATUS_LABELS, PRIORITY_COLORS, PRIORITY_LABELS, formatDateTime } from '@/lib/utils'
import { Search, Plus, ChevronLeft, ChevronRight } from 'lucide-react'

const STATUSES = ['OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'RESOLVED', 'CLOSED', 'WONT_FIX']
const PRIORITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export default function TicketList() {
  const { user } = useAuth()
  const [data, setData] = useState<PageResponse<TicketDto> | null>(null)
  const [assets, setAssets] = useState<AssetDto[]>([])
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [priority, setPriority] = useState('')
  const [assetId, setAssetId] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    assetApi.search({ size: 100 }).then(r => setAssets(r.content))
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await ticketApi.search({
        q: q || undefined,
        status: status || undefined,
        priority: priority || undefined,
        assetId: assetId || undefined,
        page,
        size: 20,
      })
      setData(res)
    } finally {
      setLoading(false)
    }
  }, [q, status, priority, assetId, page])

  useEffect(() => { load() }, [load])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">工单管理</h1>
        {canCreateTicket(user?.role) && (
          <Button asChild>
            <Link to="/tickets/new"><Plus className="h-4 w-4 mr-1" />创建工单</Link>
          </Button>
        )}
      </div>

      <form onSubmit={handleSearch} className="flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-48">
          <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
          <Input className="pl-9" placeholder="搜索工单标题..." value={q} onChange={e => setQ(e.target.value)} />
        </div>
        <Select value={status} onValueChange={v => { setStatus(v === 'ALL' ? '' : v); setPage(0) }}>
          <SelectTrigger className="w-36">
            <SelectValue placeholder="状态" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部状态</SelectItem>
            {STATUSES.map(s => (
              <SelectItem key={s} value={s}>{STATUS_LABELS[s]}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={priority} onValueChange={v => { setPriority(v === 'ALL' ? '' : v); setPage(0) }}>
          <SelectTrigger className="w-32">
            <SelectValue placeholder="优先级" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部</SelectItem>
            {PRIORITIES.map(p => <SelectItem key={p} value={p}>{PRIORITY_LABELS[p]}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={assetId} onValueChange={v => { setAssetId(v === 'ALL' ? '' : v); setPage(0) }}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="资产" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部资产</SelectItem>
            {assets.map(a => <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>)}
          </SelectContent>
        </Select>
        <Button type="submit">搜索</Button>
      </form>

      <div className="data-table">
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th>ID</th>
              <th>标题</th>
              <th>状态</th>
              <th>优先级</th>
              <th>来源</th>
              <th>资产</th>
              <th>负责人</th>
              <th>CVE</th>
              <th>创建时间</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={9} className="text-center py-10 text-gray-400">加载中...</td></tr>
            ) : data?.content.length === 0 ? (
              <tr><td colSpan={9} className="text-center py-10 text-gray-400">暂无工单</td></tr>
            ) : data?.content.map(t => (
              <tr key={t.id}>
                <td className="px-4 py-3">
                  <Link to={`/tickets/${t.id}`} className="text-blue-600 hover:underline font-mono">#{t.id}</Link>
                </td>
                <td className="px-4 py-3 max-w-xs">
                  <Link to={`/tickets/${t.id}`} className="hover:text-blue-600 truncate block">{t.title}</Link>
                </td>
                <td className="px-4 py-3">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[t.status]}`}>
                    {STATUS_LABELS[t.status]}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${PRIORITY_COLORS[t.priority]}`}>
                    {PRIORITY_LABELS[t.priority]}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`text-xs ${t.source === 'AUTO_SCAN' ? 'text-purple-600' : 'text-gray-500'}`}>
                    {t.source === 'AUTO_SCAN' ? '自动扫描' : '手动'}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-500">{t.assetName ?? '-'}</td>
                <td className="px-4 py-3 text-gray-500">{t.assigneeName ?? '未分配'}</td>
                <td className="px-4 py-3">
                  {t.cves?.length > 0 ? (
                    <span className="text-xs text-blue-600">{t.cves.length} 个</span>
                  ) : <span className="text-gray-400">-</span>}
                </td>
                <td className="px-4 py-3 text-gray-400 text-xs">{formatDateTime(t.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-500">
        <span>共 {data?.totalElements ?? 0} 条工单</span>
        {data && data.totalPages > 1 && (
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setPage(p => p - 1)} disabled={page === 0}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="px-2 py-1">第 {page + 1} / {data.totalPages} 页</span>
            <Button variant="outline" size="sm" onClick={() => setPage(p => p + 1)} disabled={data.last}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
