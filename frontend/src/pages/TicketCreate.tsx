import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams, Link } from 'react-router-dom'
import { ticketApi, assetApi, userApi, type AssetDto, type UserDto } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/lib/auth'
import { ArrowLeft, Plus, X } from 'lucide-react'

export default function TicketCreate() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { user } = useAuth()
  const canAssignOnCreate = user?.role === 'ADMIN'

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [assetId, setAssetId] = useState('')
  const [assigneeId, setAssigneeId] = useState('')
  const [cveIds, setCveIds] = useState<string[]>(
    searchParams.get('cveId') ? [searchParams.get('cveId')!] : []
  )
  const [cveInput, setCveInput] = useState('')

  const [assets, setAssets] = useState<AssetDto[]>([])
  const [users, setUsers] = useState<UserDto[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    assetApi.search({ size: 100 }).then(r => setAssets(r.content))
    if (canAssignOnCreate) {
      userApi.assignable().then(r => setUsers(r.content)).catch(() => setUsers([]))
    } else {
      setUsers([])
      setAssigneeId('')
    }
  }, [canAssignOnCreate])

  const addCve = () => {
    const id = cveInput.trim().toUpperCase()
    if (id && !cveIds.includes(id)) {
      setCveIds(prev => [...prev, id])
    }
    setCveInput('')
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) { setError('标题不能为空'); return }
    setSubmitting(true)
    setError('')
    try {
      const ticket = await ticketApi.create({
        title,
        description,
        priority,
        assetId: assetId ? Number(assetId) : null,
        assigneeId: assigneeId ? Number(assigneeId) : null,
        cveIds,
      })
      navigate(`/tickets/${ticket.id}`)
    } catch (e: any) {
      setError(e.message)
      setSubmitting(false)
    }
  }

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" asChild>
          <Link to="/tickets"><ArrowLeft className="h-4 w-4 mr-1" />返回</Link>
        </Button>
        <h1 className="text-2xl font-bold">创建工单</h1>
      </div>

      <form onSubmit={handleSubmit}>
        <Card>
          <CardContent className="pt-6 space-y-5">
            <div className="space-y-1.5">
              <Label>标题 *</Label>
              <Input placeholder="简要描述漏洞..." value={title} onChange={e => setTitle(e.target.value)} />
            </div>

            <div className="space-y-1.5">
              <Label>详细描述</Label>
              <Textarea
                placeholder="描述漏洞影响、复现步骤、修复建议等..."
                value={description}
                onChange={e => setDescription(e.target.value)}
                rows={5}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>优先级</Label>
                <Select value={priority} onValueChange={setPriority}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="CRITICAL">严重</SelectItem>
                    <SelectItem value="HIGH">高危</SelectItem>
                    <SelectItem value="MEDIUM">中危</SelectItem>
                    <SelectItem value="LOW">低危</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>关联资产</Label>
                <Select value={assetId} onValueChange={setAssetId}>
                  <SelectTrigger>
                    <SelectValue placeholder="选择资产" />
                  </SelectTrigger>
                  <SelectContent>
                    {assets.map(a => <SelectItem key={a.id} value={String(a.id)}>{a.name}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {canAssignOnCreate && (
              <div className="space-y-1.5">
                <Label>负责人</Label>
                <Select value={assigneeId} onValueChange={setAssigneeId}>
                  <SelectTrigger>
                    <SelectValue placeholder="选择负责人（可选）" />
                  </SelectTrigger>
                  <SelectContent>
                    {users.filter(u => u.active).map(u => (
                      <SelectItem key={u.id} value={String(u.id)}>
                        {u.fullName} — {u.devGroupName ?? '无组'}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* CVE Association */}
            <div className="space-y-1.5">
              <Label>关联 CVE</Label>
              <div className="flex flex-wrap gap-2 mb-2">
                {cveIds.map(id => (
                  <span key={id} className="inline-flex items-center gap-1 px-2.5 py-1 bg-blue-50 text-blue-800 border border-blue-200 rounded-full text-xs font-mono">
                    {id}
                    <button type="button" onClick={() => setCveIds(prev => prev.filter(c => c !== id))}>
                      <X className="h-3 w-3 hover:text-red-600" />
                    </button>
                  </span>
                ))}
              </div>
              <div className="flex gap-2">
                <Input
                  placeholder="CVE-YYYY-NNNNN"
                  value={cveInput}
                  onChange={e => setCveInput(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addCve() } }}
                  className="font-mono text-sm"
                />
                <Button type="button" variant="outline" size="sm" onClick={addCve}>
                  <Plus className="h-3.5 w-3.5" />
                </Button>
              </div>
            </div>

            {error && <p className="text-sm text-red-600">{error}</p>}

            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="outline" onClick={() => navigate('/tickets')}>取消</Button>
              <Button type="submit" disabled={submitting}>
                {submitting ? '提交中...' : '创建工单'}
              </Button>
            </div>
          </CardContent>
        </Card>
      </form>
    </div>
  )
}
