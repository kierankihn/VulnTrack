import { useEffect, useState } from 'react'
import { devGroupApi, userApi, type DevGroupDto, type UserDto } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Plus, Pencil, Trash2, Users, Building2, Crown } from 'lucide-react'

const EMPTY_FORM = { name: '', description: '', leaderId: '' }

export default function DevGroups() {
  const [groups, setGroups] = useState<DevGroupDto[]>([])
  const [users, setUsers] = useState<UserDto[]>([])
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DevGroupDto | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [error, setError] = useState<string | null>(null)

  const load = () => devGroupApi.findAll().then(setGroups)

  useEffect(() => {
    load()
    userApi.search({ size: 500 }).then(r => setUsers(r.content)).catch(() => setUsers([]))
  }, [])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setError(null)
    setOpen(true)
  }
  const openEdit = (g: DevGroupDto) => {
    setEditing(g)
    setForm({ name: g.name, description: g.description ?? '', leaderId: g.leaderId ? String(g.leaderId) : '' })
    setError(null)
    setOpen(true)
  }

  const handleSave = async () => {
    setError(null)
    const payload = {
      name: form.name,
      description: form.description,
      leaderId: form.leaderId ? Number(form.leaderId) : null,
    }
    try {
      if (editing) await devGroupApi.update(editing.id, payload)
      else await devGroupApi.create(payload)
      setOpen(false)
      load()
    } catch (e: any) {
      setError(e.message || '保存失败')
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('确认删除此开发组？')) return
    try {
      await devGroupApi.delete(id)
      load()
    } catch (e: any) {
      alert(e.message || '删除失败')
    }
  }

  const leaderCandidates = users.filter(u => {
    if (!u.active) return false
    if (u.role === 'GROUP_LEAD' && u.devGroupId !== editing?.id) return false
    if (!editing) return true
    return u.devGroupId === editing.id || u.role === 'ADMIN'
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-emerald-500 to-teal-600 flex items-center justify-center shadow-sm">
            <Building2 className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">开发组管理</h1>
            <p className="text-sm text-gray-500 mt-0.5">按团队组织用户与资产</p>
          </div>
        </div>
        <Button onClick={openCreate} className="shadow-sm"><Plus className="h-4 w-4 mr-1" />创建开发组</Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {groups.length === 0 && (
          <div className="col-span-full text-center py-16 text-gray-400 bg-white rounded-xl border border-dashed border-gray-200">
            暂无开发组
          </div>
        )}
        {groups.map(g => (
          <Card key={g.id} className="group hover:shadow-lg hover:-translate-y-0.5 transition-all duration-200 border-gray-200/80 overflow-hidden">
            <div className="h-1 bg-gradient-to-r from-emerald-400 to-teal-500" />
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center">
                    <Building2 className="h-4 w-4" />
                  </div>
                  <div>
                    <CardTitle className="text-base">{g.name}</CardTitle>
                    {g.leaderName && (
                      <div className="flex items-center gap-1 text-xs text-amber-600 mt-0.5">
                        <Crown className="h-3 w-3" />
                        <span>组长：{g.leaderName}</span>
                      </div>
                    )}
                  </div>
                </div>
                <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  <Button variant="ghost" size="icon" onClick={() => openEdit(g)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => handleDelete(g.id)}>
                    <Trash2 className="h-3.5 w-3.5 text-red-500" />
                  </Button>
                </div>
              </div>
              {g.description && <p className="text-sm text-gray-500 mt-2 line-clamp-2">{g.description}</p>}
            </CardHeader>
            <CardContent>
              <div className="flex gap-4 text-sm pt-2 border-t border-gray-100">
                <div className="flex items-center gap-1.5 text-gray-600">
                  <Users className="h-4 w-4 text-gray-400" />
                  <span><span className="font-semibold text-gray-900">{g.memberCount}</span> 成员</span>
                </div>
                <div className="flex items-center gap-1.5 text-gray-600">
                  <span><span className="font-semibold text-gray-900">{g.assetCount}</span> 资产</span>
                </div>
              </div>
              {g.members && g.members.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1">
                  {g.members.map(m => (
                    <span key={m.id} className="inline-flex px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">
                      {m.fullName}
                    </span>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{editing ? '编辑开发组' : '创建开发组'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="space-y-1.5">
              <Label>组名 *</Label>
              <Input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>描述</Label>
              <Textarea value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} rows={2} />
            </div>
            <div className="space-y-1.5">
              <Label className="flex items-center gap-1">
                <Crown className="h-3.5 w-3.5 text-amber-500" />
                组长
              </Label>
              <Select
                value={form.leaderId || 'NONE'}
                onValueChange={v => setForm(f => ({ ...f, leaderId: v === 'NONE' ? '' : v }))}
              >
                <SelectTrigger><SelectValue placeholder="不设置" /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">不设置</SelectItem>
                  {leaderCandidates.map(u => (
                    <SelectItem key={u.id} value={String(u.id)}>
                      {u.fullName}（{u.username}）
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {editing && (
                <p className="text-xs text-gray-400">
                  仅可从本组成员中选择；设为组长会自动将其角色升级为「组长」
                </p>
              )}
            </div>

            {error && (
              <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
                {error}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setOpen(false)}>取消</Button>
              <Button onClick={handleSave} disabled={!form.name}>{editing ? '保存' : '创建'}</Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
