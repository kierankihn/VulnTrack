import { useEffect, useState, useCallback } from 'react'
import { Link } from 'react-router-dom'
import { cveApi, type CveDto, type PageResponse } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { SEVERITY_COLORS, formatDate } from '@/lib/utils'
import { Search, ChevronLeft, ChevronRight } from 'lucide-react'

const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'NONE']

export default function CveList() {
  const [data, setData] = useState<PageResponse<CveDto> | null>(null)
  const [q, setQ] = useState('')
  const [severity, setSeverity] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await cveApi.search({ q: q || undefined, severity: severity || undefined, page, size: 20 })
      setData(res)
    } finally {
      setLoading(false)
    }
  }, [q, severity, page])

  useEffect(() => { load() }, [load])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    load()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">CVE 数据库</h1>
        <span className="text-sm text-gray-500">共 {data?.totalElements ?? 0} 条记录</span>
      </div>

      <form onSubmit={handleSearch} className="flex gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
          <Input
            className="pl-9"
            placeholder="搜索 CVE ID 或描述..."
            value={q}
            onChange={e => setQ(e.target.value)}
          />
        </div>
        <Select value={severity} onValueChange={v => { setSeverity(v === 'ALL' ? '' : v); setPage(0) }}>
          <SelectTrigger className="w-36">
            <SelectValue placeholder="严重程度" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">全部</SelectItem>
            {SEVERITIES.map(s => (
              <SelectItem key={s} value={s}>
                <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${SEVERITY_COLORS[s]}`}>
                  {s}
                </span>
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button type="submit">搜索</Button>
      </form>

      <div className="data-table">
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th>CVE ID</th>
              <th>描述</th>
              <th>严重程度</th>
              <th>CVSS v3</th>
              <th>发布日期</th>
              <th>工单数</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className="text-center py-10 text-gray-400">加载中...</td></tr>
            ) : data?.content.length === 0 ? (
              <tr><td colSpan={6} className="text-center py-10 text-gray-400">暂无数据</td></tr>
            ) : data?.content.map(cve => (
              <tr key={cve.id}>
                <td className="px-4 py-3">
                  <Link to={`/cves/${cve.id}`} className="font-mono text-blue-600 hover:underline">
                    {cve.cveId}
                  </Link>
                </td>
                <td className="px-4 py-3 max-w-sm">
                  <p className="truncate text-gray-700">{cve.description || '-'}</p>
                </td>
                <td className="px-4 py-3">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium border ${SEVERITY_COLORS[cve.severity]}`}>
                    {cve.severity}
                  </span>
                </td>
                <td className="px-4 py-3 font-mono">
                  {cve.cvssV3Score != null ? cve.cvssV3Score.toFixed(1) : '-'}
                </td>
                <td className="px-4 py-3 text-gray-500">{formatDate(cve.publishedDate)}</td>
                <td className="px-4 py-3">
                  {cve.ticketCount > 0 ? (
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                      {cve.ticketCount}
                    </span>
                  ) : (
                    <span className="text-gray-400">0</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-500">
            第 {page + 1} / {data.totalPages} 页
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => setPage(p => p - 1)} disabled={page === 0}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button variant="outline" size="sm" onClick={() => setPage(p => p + 1)} disabled={data.last}>
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
