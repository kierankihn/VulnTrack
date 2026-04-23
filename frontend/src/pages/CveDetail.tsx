import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { cveApi, type CveDto, type TicketDto } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { SEVERITY_COLORS, STATUS_COLORS, STATUS_LABELS, PRIORITY_LABELS, PRIORITY_COLORS, formatDate, formatDateTime } from '@/lib/utils'
import { ArrowLeft, ExternalLink, TicketCheck } from 'lucide-react'

export default function CveDetail() {
  const { id } = useParams<{ id: string }>()
  const [cve, setCve] = useState<CveDto | null>(null)
  const [tickets, setTickets] = useState<TicketDto[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!id) return
    Promise.all([
      cveApi.getById(Number(id)).then(setCve),
      cveApi.getTickets(Number(id)).then(setTickets),
    ]).finally(() => setLoading(false))
  }, [id])

  if (loading) return <div className="py-12 text-center text-gray-400">加载中...</div>
  if (!cve) return <div className="py-12 text-center text-gray-400">未找到 CVE</div>

  const refs = cve.references?.split('\n').filter(Boolean) ?? []

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/cves"><ArrowLeft className="h-4 w-4 mr-1" />返回</Link>
        </Button>
      </div>

      {/* Header */}
      <div className="bg-white rounded-lg border p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold font-mono text-gray-900">{cve.cveId}</h1>
            <div className="flex items-center gap-3 mt-2">
              <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold border ${SEVERITY_COLORS[cve.severity]}`}>
                {cve.severity}
              </span>
              {cve.cvssV3Score != null && (
                <span className="text-sm font-mono font-semibold text-gray-700">
                  CVSS v3: {cve.cvssV3Score.toFixed(1)}
                </span>
              )}
              {cve.cvssV2Score != null && (
                <span className="text-sm font-mono text-gray-500">
                  CVSS v2: {cve.cvssV2Score.toFixed(1)}
                </span>
              )}
            </div>
          </div>
          <div className="text-right text-sm text-gray-400">
            <div>发布：{formatDate(cve.publishedDate)}</div>
            <div>更新：{formatDate(cve.lastModifiedDate)}</div>
            <div>同步：{formatDateTime(cve.syncedAt)}</div>
          </div>
        </div>

        {cve.description && (
          <p className="mt-4 text-gray-700 leading-relaxed">{cve.description}</p>
        )}

        {cve.cvssV3Vector && (
          <div className="mt-3 font-mono text-xs text-gray-500 bg-gray-50 rounded p-2">
            {cve.cvssV3Vector}
          </div>
        )}
      </div>

      {/* References */}
      {refs.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">参考链接</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="space-y-1">
              {refs.slice(0, 10).map((r, i) => (
                <li key={i} className="flex items-center gap-2">
                  <ExternalLink className="h-3 w-3 text-gray-400 flex-shrink-0" />
                  <a href={r} target="_blank" rel="noreferrer"
                    className="text-sm text-blue-600 hover:underline truncate">{r}</a>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}

      {/* Related Tickets */}
      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <CardTitle className="text-base flex items-center gap-2">
            <TicketCheck className="h-4 w-4" />
            相关工单 ({tickets.length})
          </CardTitle>
          <Button size="sm" asChild>
            <Link to={`/tickets/new?cveId=${cve.cveId}`}>创建工单</Link>
          </Button>
        </CardHeader>
        <CardContent>
          {tickets.length === 0 ? (
            <p className="text-sm text-gray-400">暂无相关工单</p>
          ) : (
            <table className="w-full text-sm">
              <thead className="border-b">
                <tr>
                  <th className="text-left pb-2 font-medium text-gray-500">ID</th>
                  <th className="text-left pb-2 font-medium text-gray-500">标题</th>
                  <th className="text-left pb-2 font-medium text-gray-500">状态</th>
                  <th className="text-left pb-2 font-medium text-gray-500">优先级</th>
                  <th className="text-left pb-2 font-medium text-gray-500">资产</th>
                  <th className="text-left pb-2 font-medium text-gray-500">负责人</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {tickets.map(t => (
                  <tr key={t.id} className="hover:bg-gray-50">
                    <td className="py-2">
                      <Link to={`/tickets/${t.id}`} className="text-blue-600 hover:underline">
                        #{t.id}
                      </Link>
                    </td>
                    <td className="py-2 max-w-xs"><span className="truncate block">{t.title}</span></td>
                    <td className="py-2">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[t.status]}`}>
                        {STATUS_LABELS[t.status]}
                      </span>
                    </td>
                    <td className="py-2">
                      <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${PRIORITY_COLORS[t.priority]}`}>
                        {PRIORITY_LABELS[t.priority]}
                      </span>
                    </td>
                    <td className="py-2 text-gray-500">{t.assetName ?? '-'}</td>
                    <td className="py-2 text-gray-500">{t.assigneeName ?? '未分配'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
