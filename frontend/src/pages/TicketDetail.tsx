import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { ticketApi, userApi, cveApi, type TicketDto, type UserDto } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  STATUS_COLORS, STATUS_LABELS, PRIORITY_COLORS, PRIORITY_LABELS,
  SEVERITY_COLORS, formatDateTime, formatJsonForDisplay
} from '@/lib/utils'
import { useAuth, canAssignTicket } from '@/lib/auth'
import { ArrowLeft, User, Plus, X, History, ArrowRight } from 'lucide-react'

const TRANSITION_LABELS: Record<string, string> = {
  IN_PROGRESS: '开始处理',
  IN_REVIEW:   '提交审核',
  RESOLVED:    '标记已修复',
  CLOSED:      '关闭工单',
  WONT_FIX:    '标记不修复',
}

export default function TicketDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()
  const canAssign = canAssignTicket(user?.role)
  const [ticket, setTicket] = useState<TicketDto | null>(null)
  const [loading, setLoading] = useState(true)

  const [transitionOpen, setTransitionOpen] = useState(false)
  const [targetStatus, setTargetStatus] = useState('')
  const [transitionComment, setTransitionComment] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [cveInput, setCveInput] = useState('')
  const [cveError, setCveError] = useState('')

  const [users, setUsers] = useState<UserDto[]>([])
  const [assigneeOpen, setAssigneeOpen] = useState(false)
  const [selectedAssignee, setSelectedAssignee] = useState('')

  useEffect(() => {
    if (!id) return
    ticketApi.getById(Number(id)).then(t => {
      setTicket(t)
      setLoading(false)
    })
    if (canAssign) {
      userApi.assignable().then(r => setUsers(r.content)).catch(() => setUsers([]))
    } else {
      setUsers([])
    }
  }, [id, canAssign])

  const handleTransition = async () => {
    if (!ticket || !targetStatus) return
    setSubmitting(true)
    try {
      const updated = await ticketApi.transition(ticket.id, { targetStatus, comment: transitionComment })
      setTicket(updated)
      setTransitionOpen(false)
      setTransitionComment('')
    } catch (e: any) {
      alert(e.message)
    } finally {
      setSubmitting(false)
    }
  }

  const handleAddCve = async () => {
    if (!ticket || !cveInput.trim()) return
    setCveError('')
    try {
      const updated = await ticketApi.addCve(ticket.id, cveInput.trim().toUpperCase())
      setTicket(updated)
      setCveInput('')
    } catch (e: any) {
      setCveError(e.message)
    }
  }

  const handleRemoveCve = async (cveId: string) => {
    if (!ticket) return
    const updated = await ticketApi.removeCve(ticket.id, cveId)
    setTicket(updated)
  }

  const handleReassign = async () => {
    if (!ticket || !selectedAssignee) return
    const updated = await ticketApi.updateAssignee(ticket.id, Number(selectedAssignee))
    setTicket(updated)
    setAssigneeOpen(false)
    setSelectedAssignee('')
  }

  if (loading) return <div className="py-12 text-center text-gray-400">加载中...</div>
  if (!ticket) return <div className="py-12 text-center text-gray-400">工单不存在</div>

  const isTerminal = ticket.status === 'CLOSED' || ticket.status === 'WONT_FIX'

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/tickets"><ArrowLeft className="h-4 w-4 mr-1" />返回</Link>
        </Button>
      </div>

      {/* Header */}
      <div className="bg-white rounded-lg border p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-sm font-mono text-gray-400">#{ticket.id}</span>
              <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[ticket.status]}`}>
                {STATUS_LABELS[ticket.status]}
              </span>
              <span className={`inline-flex px-2.5 py-0.5 rounded-full text-xs font-semibold ${PRIORITY_COLORS[ticket.priority]}`}>
                {PRIORITY_LABELS[ticket.priority]}
              </span>
              <span className={`text-xs px-2 py-0.5 rounded ${ticket.source === 'AUTO_SCAN' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'}`}>
                {ticket.source === 'AUTO_SCAN' ? '自动扫描' : '手动创建'}
              </span>
            </div>
            <h1 className="text-xl font-bold text-gray-900">{ticket.title}</h1>
            {ticket.description && (
              <p className="mt-2 text-gray-600 text-sm whitespace-pre-wrap">{ticket.description}</p>
            )}
          </div>
        </div>

        {/* Meta */}
        <div className="grid grid-cols-3 gap-4 mt-4 pt-4 border-t text-sm">
          <div>
            <span className="text-gray-400">资产：</span>
            {ticket.assetId ? (
              <Link to={`/assets`} className="text-blue-600 hover:underline">{ticket.assetName}</Link>
            ) : <span className="text-gray-500">-</span>}
          </div>
          <div className="flex items-center gap-2">
            <span className="text-gray-400">负责人：</span>
            <span>{ticket.assigneeName ?? '未分配'}</span>
            {canAssign && !isTerminal && (
              <button onClick={() => setAssigneeOpen(true)} className="text-blue-500 hover:text-blue-700">
                <User className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
          <div>
            <span className="text-gray-400">报告人：</span>
            <span>{ticket.reporterName ?? '-'}</span>
          </div>
          <div>
            <span className="text-gray-400">创建：</span>
            <span className="text-gray-600">{formatDateTime(ticket.createdAt)}</span>
          </div>
          <div>
            <span className="text-gray-400">更新：</span>
            <span className="text-gray-600">{formatDateTime(ticket.updatedAt)}</span>
          </div>
          {ticket.resolvedAt && (
            <div>
              <span className="text-gray-400">修复：</span>
              <span className="text-gray-600">{formatDateTime(ticket.resolvedAt)}</span>
            </div>
          )}
        </div>

        {/* Transition Buttons */}
        {!isTerminal && ticket.allowedTransitions.length > 0 && (
          <div className="flex gap-2 mt-4 pt-4 border-t flex-wrap">
            {ticket.allowedTransitions.map(ts => (
              <Button
                key={ts}
                size="sm"
                variant={ts === 'WONT_FIX' ? 'destructive' : ts === 'CLOSED' ? 'secondary' : 'default'}
                onClick={() => { setTargetStatus(ts); setTransitionOpen(true) }}
              >
                <ArrowRight className="h-3.5 w-3.5 mr-1.5" />
                {TRANSITION_LABELS[ts] || ts}
              </Button>
            ))}
          </div>
        )}
      </div>

      {/* CVE Panel */}
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">关联 CVE</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2 mb-3">
            {ticket.cves?.length === 0 && <span className="text-sm text-gray-400">暂无关联 CVE</span>}
            {ticket.cves?.map(cve => (
              <div key={cve.id} className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full border text-xs font-medium ${SEVERITY_COLORS[cve.severity]}`}>
                <Link to={`/cves/${cve.id}`} className="hover:underline font-mono">{cve.cveId}</Link>
                <span className="text-gray-400">({cve.severity})</span>
                {!isTerminal && (
                  <button onClick={() => handleRemoveCve(cve.cveId)} className="hover:text-red-600 ml-1">
                    <X className="h-3 w-3" />
                  </button>
                )}
              </div>
            ))}
          </div>
          {!isTerminal && (
            <div className="flex gap-2 mt-2">
              <Input
                placeholder="输入 CVE ID，如 CVE-2021-44228"
                value={cveInput}
                onChange={e => { setCveInput(e.target.value); setCveError('') }}
                className="max-w-xs h-8 text-sm font-mono"
                onKeyDown={e => e.key === 'Enter' && handleAddCve()}
              />
              <Button size="sm" variant="outline" onClick={handleAddCve}>
                <Plus className="h-3.5 w-3.5 mr-1" />关联
              </Button>
            </div>
          )}
          {cveError && <p className="text-xs text-red-500 mt-1">{cveError}</p>}
        </CardContent>
      </Card>

      {/* Status History */}
      {ticket.statusHistory?.length > 0 && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base flex items-center gap-2">
              <History className="h-4 w-4" />流转历史
            </CardTitle>
          </CardHeader>
          <CardContent>
            <ol className="space-y-2">
              {ticket.statusHistory.map((h, i) => {
                const parts = h.split(' | ')
                return (
                  <li key={i} className="flex gap-3 text-sm">
                    <span className="text-gray-400 whitespace-nowrap text-xs">{parts[0]}</span>
                    <span className="text-gray-700">{parts.slice(1).join(' · ')}</span>
                  </li>
                )
              })}
            </ol>
          </CardContent>
        </Card>
      )}

      {/* Scan Info */}
      {ticket.scanInfo && (
        <Card>
          <CardHeader className="pb-3">
            <CardTitle className="text-base">扫描信息</CardTitle>
          </CardHeader>
          <CardContent>
            <pre className="text-xs bg-gray-50 rounded p-3 overflow-x-auto">
              {formatJsonForDisplay(ticket.scanInfo)}
            </pre>
          </CardContent>
        </Card>
      )}

      {/* Transition Dialog */}
      <Dialog open={transitionOpen} onOpenChange={setTransitionOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>状态流转：{TRANSITION_LABELS[targetStatus]}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 pt-2">
            <p className="text-sm text-gray-500">
              将工单状态从 <strong>{STATUS_LABELS[ticket.status]}</strong> 变更为 <strong>{STATUS_LABELS[targetStatus]}</strong>
            </p>
            <div>
              <Label className="mb-1.5 block">备注（可选）</Label>
              <Textarea
                placeholder="填写变更原因或备注..."
                value={transitionComment}
                onChange={e => setTransitionComment(e.target.value)}
                rows={3}
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setTransitionOpen(false)}>取消</Button>
              <Button onClick={handleTransition} disabled={submitting}>
                {submitting ? '提交中...' : '确认'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Reassign Dialog */}
      <Dialog open={assigneeOpen} onOpenChange={setAssigneeOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>重新分配负责人</DialogTitle></DialogHeader>
          <div className="space-y-3 pt-2">
            <Select value={selectedAssignee} onValueChange={setSelectedAssignee}>
              <SelectTrigger>
                <SelectValue placeholder="选择负责人" />
              </SelectTrigger>
              <SelectContent>
                {users.filter(u => u.active).map(u => (
                  <SelectItem key={u.id} value={String(u.id)}>
                    {u.fullName} ({u.username}) — {u.devGroupName ?? '无组'}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setAssigneeOpen(false)}>取消</Button>
              <Button onClick={handleReassign} disabled={!selectedAssignee}>确认</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
