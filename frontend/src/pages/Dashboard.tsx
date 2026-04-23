import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cveApi, ticketApi, type CveStats, type TicketStats } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { useAuth, canCreateTicket, canSyncCves } from '@/lib/auth'
import {
  Shield, TicketCheck, AlertTriangle, CheckCircle, Clock,
  XCircle, RefreshCw, Database, TrendingUp, Activity
} from 'lucide-react'

interface StatCardProps {
  label: string
  value: number | string | undefined
  icon?: React.ElementType
  color: string
  bg: string
  border?: string
}

function StatCard({ label, value, icon: Icon, color, bg, border }: StatCardProps) {
  return (
    <div className={`rounded-xl border p-5 ${bg} ${border ?? 'border-gray-200'} shadow-sm`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">{label}</p>
          <p className={`text-3xl font-bold ${color}`}>{value ?? '—'}</p>
        </div>
        {Icon && <Icon className={`h-8 w-8 ${color} opacity-20`} />}
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const [cveStats, setCveStats] = useState<CveStats | null>(null)
  const [ticketStats, setTicketStats] = useState<TicketStats | null>(null)
  const [syncing, setSyncing] = useState(false)
  const [syncResult, setSyncResult] = useState<{ ok: boolean; msg: string } | null>(null)

  useEffect(() => {
    cveApi.getStats().then(setCveStats).catch(console.error)
    ticketApi.getStats().then(setTicketStats).catch(console.error)
  }, [])

  const handleSync = async () => {
    setSyncing(true)
    setSyncResult(null)
    try {
      const r = await cveApi.triggerSync()
      setSyncResult({
        ok: !r.error,
        msg: r.error ? `同步失败: ${r.error}` : `同步完成：新增 ${r.added} 条，更新 ${r.updated} 条`,
      })
      cveApi.getStats().then(setCveStats)
    } catch (e: any) {
      setSyncResult({ ok: false, msg: `同步失败: ${e.message}` })
    } finally {
      setSyncing(false)
    }
  }

  return (
    <div className="space-y-7 max-w-6xl">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">系统总览</h1>
          <p className="text-sm text-gray-500 mt-0.5">漏洞追踪与工单管理中心</p>
        </div>
        {canSyncCves(user?.role) && (
          <Button onClick={handleSync} disabled={syncing} variant="outline" size="sm" className="gap-2">
            <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
            {syncing ? '同步中...' : '同步 NVD 数据'}
          </Button>
        )}
      </div>

      {syncResult && (
        <div className={`flex items-center gap-2 rounded-lg border px-4 py-3 text-sm ${
          syncResult.ok
            ? 'bg-green-50 border-green-200 text-green-800'
            : 'bg-red-50 border-red-200 text-red-800'
        }`}>
          {syncResult.ok ? <CheckCircle className="h-4 w-4" /> : <XCircle className="h-4 w-4" />}
          {syncResult.msg}
        </div>
      )}

      {/* CVE Stats */}
      <section>
        <div className="flex items-center gap-2 mb-3">
          <Database className="h-4 w-4 text-gray-400" />
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider">CVE 数据库</h2>
        </div>
        <div className="grid grid-cols-5 gap-4">
          <StatCard label="CVE 总数"  value={cveStats?.total}    icon={Database}      color="text-gray-800"   bg="bg-white" />
          <StatCard label="严重"      value={cveStats?.critical} icon={XCircle}       color="text-red-700"    bg="bg-red-50"    border="border-red-100" />
          <StatCard label="高危"      value={cveStats?.high}     icon={AlertTriangle} color="text-orange-700" bg="bg-orange-50" border="border-orange-100" />
          <StatCard label="中危"      value={cveStats?.medium}   icon={Activity}      color="text-yellow-700" bg="bg-yellow-50" border="border-yellow-100" />
          <StatCard label="低危"      value={cveStats?.low}      icon={Shield}        color="text-green-700"  bg="bg-green-50"  border="border-green-100" />
        </div>
      </section>

      {/* Ticket Stats */}
      <section>
        <div className="flex items-center gap-2 mb-3">
          <TicketCheck className="h-4 w-4 text-gray-400" />
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider">工单状态</h2>
        </div>
        <div className="grid grid-cols-4 gap-4">
          <StatCard label="待处理" value={ticketStats?.open}       icon={Clock}         color="text-blue-700"   bg="bg-blue-50"   border="border-blue-100" />
          <StatCard label="处理中" value={ticketStats?.inProgress}  icon={TrendingUp}    color="text-purple-700" bg="bg-purple-50" border="border-purple-100" />
          <StatCard label="审核中" value={ticketStats?.inReview}    icon={AlertTriangle} color="text-yellow-700" bg="bg-yellow-50" border="border-yellow-100" />
          <StatCard label="已修复" value={ticketStats?.resolved}    icon={CheckCircle}   color="text-green-700"  bg="bg-green-50"  border="border-green-100" />
        </div>
      </section>

      {/* Priority alerts */}
      <section>
        <div className="flex items-center gap-2 mb-3">
          <AlertTriangle className="h-4 w-4 text-gray-400" />
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider">优先级预警</h2>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="rounded-xl border border-red-200 bg-red-50 p-5 flex items-center gap-4 shadow-sm">
            <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
              <XCircle className="h-6 w-6 text-red-600" />
            </div>
            <div>
              <p className="text-3xl font-bold text-red-700">{ticketStats?.critical ?? '—'}</p>
              <p className="text-sm text-red-600 mt-0.5">严重优先级工单</p>
            </div>
          </div>
          <div className="rounded-xl border border-orange-200 bg-orange-50 p-5 flex items-center gap-4 shadow-sm">
            <div className="w-12 h-12 rounded-full bg-orange-100 flex items-center justify-center flex-shrink-0">
              <AlertTriangle className="h-6 w-6 text-orange-600" />
            </div>
            <div>
              <p className="text-3xl font-bold text-orange-700">{ticketStats?.high ?? '—'}</p>
              <p className="text-sm text-orange-600 mt-0.5">高危优先级工单</p>
            </div>
          </div>
        </div>
      </section>

      {/* Quick actions */}
      <section>
        <div className="flex gap-3">
          {canCreateTicket(user?.role) && (
            <Button asChild className="bg-blue-600 hover:bg-blue-700">
              <Link to="/tickets/new">创建工单</Link>
            </Button>
          )}
          <Button asChild variant="outline"><Link to="/cves">浏览 CVE</Link></Button>
          <Button asChild variant="outline"><Link to="/tickets">查看工单</Link></Button>
        </div>
      </section>
    </div>
  )
}
