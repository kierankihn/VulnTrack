import { useEffect, useState } from 'react'
import { assetApi, devGroupApi, type AssetDto, type DevGroupDto } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { formatDateTime, formatRepoUrlLabel } from '@/lib/utils'
import { useAuth, canManageAssets } from '@/lib/auth'
import { Plus, Pencil, Trash2, ExternalLink, Search } from 'lucide-react'

const EMPTY_FORM = { name: '', description: '', repoUrl: '', projectName: '', devGroupId: '' }

export default function Assets() {
  const { user } = useAuth()
  const canManage = canManageAssets(user?.role)
  const [assets, setAssets] = useState<AssetDto[]>([])
  const [groups, setGroups] = useState<DevGroupDto[]>([])
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<AssetDto | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const load = (query = q) =>
    assetApi.search({ q: query || undefined, size: 100 })
      .then(r => setAssets(r.content))

  useEffect(() => {
    load()
    devGroupApi.findAll().then(setGroups)
  }, [])

  const openCreate = () => { setEditing(null); setForm(EMPTY_FORM); setOpen(true) }
  const openEdit = (a: AssetDto) => {
    setEditing(a)
    setForm({
      name: a.name, description: a.description ?? '', repoUrl: a.repoUrl ?? '',
      projectName: a.projectName ?? '', devGroupId: a.devGroupId ? String(a.devGroupId) : ''
    })
    setOpen(true)
  }

  const handleSave = async () => {
    const payload = {
      name: form.name, description: form.description,
      repoUrl: form.repoUrl, projectName: form.projectName,
      devGroupId: form.devGroupId ? Number(form.devGroupId) : null,
    }
    if (editing) {
      await assetApi.update(editing.id, payload)
    } else {
      await assetApi.create(payload)
    }
    setOpen(false)
    load()
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此资产？')) return
    await assetApi.delete(id)
    load()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">资产管理</h1>
        {canManage ? (
          <Button onClick={openCreate}><Plus className="h-4 w-4 mr-1" />添加资产</Button>
        ) : (
          <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-500">
            只读视图
          </span>
        )}
      </div>

      <div className="flex gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-3 h-4 w-4 text-gray-400" />
          <Input className="pl-9" placeholder="搜索资产..." value={q}
            onChange={e => { setQ(e.target.value); load(e.target.value) }} />
        </div>
      </div>

      <div className="data-table">
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th>资产名称</th>
              <th>项目名</th>
              <th>仓库</th>
              <th>开发组</th>
              <th>活跃工单</th>
              <th>创建时间</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {assets.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-10 text-gray-400">暂无资产</td></tr>
            ) : assets.map(a => (
              <tr key={a.id}>
                <td className="px-4 py-3 font-medium">{a.name}</td>
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{a.projectName ?? '-'}</td>
                <td className="px-4 py-3">
                  {a.repoUrl ? (
                    <a href={a.repoUrl} target="_blank" rel="noreferrer"
                      className="flex items-center gap-1 text-blue-600 hover:underline text-xs">
                      <ExternalLink className="h-3 w-3" />{formatRepoUrlLabel(a.repoUrl)}
                    </a>
                  ) : <span className="text-gray-400">-</span>}
                </td>
                <td className="px-4 py-3">{a.devGroupName ?? <span className="text-gray-400">未分配</span>}</td>
                <td className="px-4 py-3">
                  {a.openTicketCount > 0 ? (
                    <span className="inline-flex px-2 py-0.5 rounded-full text-xs bg-orange-100 text-orange-700">
                      {a.openTicketCount}
                    </span>
                  ) : <span className="text-gray-400">0</span>}
                </td>
                <td className="px-4 py-3 text-gray-400 text-xs">{formatDateTime(a.createdAt)}</td>
                <td className="px-4 py-3">
                  {canManage && (
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(a)}>
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => handleDelete(a.id)}>
                        <Trash2 className="h-3.5 w-3.5 text-red-500" />
                      </Button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Dialog open={canManage && open} onOpenChange={setOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? '编辑资产' : '添加资产'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="space-y-1.5">
              <Label>资产名称 *</Label>
              <Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>项目名称（用于 CI/CD 匹配）</Label>
              <Input placeholder="my-project" value={form.projectName}
                onChange={e => setForm(f => ({ ...f, projectName: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>仓库 URL</Label>
              <Input placeholder="https://github.com/org/repo" value={form.repoUrl}
                onChange={e => setForm(f => ({ ...f, repoUrl: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>开发组</Label>
              <Select value={form.devGroupId} onValueChange={v => setForm(f => ({ ...f, devGroupId: v === 'NONE' ? '' : v }))}>
                <SelectTrigger><SelectValue placeholder="选择开发组" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">不分配</SelectItem>
                  {groups.map(g => <SelectItem key={g.id} value={String(g.id)}>{g.name}</SelectItem>)}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1.5">
              <Label>描述</Label>
              <Textarea value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} rows={2} />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setOpen(false)}>取消</Button>
              <Button onClick={handleSave} disabled={!form.name}>{editing ? '保存' : '创建'}</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
