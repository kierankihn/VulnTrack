import { useEffect, useState, useCallback } from 'react'
import { userApi, devGroupApi, type UserDto, type DevGroupDto } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { formatDateTime } from '@/lib/utils'
import { Plus, Pencil, Search, UserCheck, UserX, Users as UsersIcon, Eye, EyeOff, KeyRound } from 'lucide-react'

const EMPTY_FORM = { username: '', email: '', fullName: '', role: 'DEVELOPER', devGroupId: '', password: '' }

const ROLE_OPTIONS = [
  { value: 'ADMIN',      label: '管理员',   badge: 'bg-purple-100 text-purple-700 ring-1 ring-purple-200' },
  { value: 'GROUP_LEAD', label: '组长',     badge: 'bg-amber-100 text-amber-700 ring-1 ring-amber-200' },
  { value: 'DEVELOPER',  label: '开发者',   badge: 'bg-blue-100 text-blue-700 ring-1 ring-blue-200' },
  { value: 'TESTER',     label: '测试员',   badge: 'bg-teal-100 text-teal-700 ring-1 ring-teal-200' },
]

const EDITABLE_ROLES = ROLE_OPTIONS.filter(role => role.value !== 'GROUP_LEAD')

const roleMeta = (role: string) => ROLE_OPTIONS.find(r => r.value === role) ?? { label: role, badge: 'bg-gray-100 text-gray-600' }

export default function Users() {
  const [users, setUsers] = useState<UserDto[]>([])
  const [groups, setGroups] = useState<DevGroupDto[]>([])
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<UserDto | null>(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [resetOpen, setResetOpen] = useState(false)
  const [resetTarget, setResetTarget] = useState<UserDto | null>(null)
  const [resetPassword, setResetPassword] = useState('')
  const [showResetPassword, setShowResetPassword] = useState(false)
  const [resetSaving, setResetSaving] = useState(false)
  const [resetError, setResetError] = useState<string | null>(null)

  const load = useCallback(async (query = q) => {
    setLoading(true)
    const r = await userApi.search({ q: query || undefined, size: 100 })
    setUsers(r.content)
    setLoading(false)
  }, [q])

  useEffect(() => {
    load()
    devGroupApi.findAll().then(setGroups)
  }, [])

  const openCreate = () => { setEditing(null); setForm(EMPTY_FORM); setError(null); setShowPassword(false); setOpen(true) }
  const openEdit = (u: UserDto) => {
    setEditing(u)
    setForm({
      username: u.username, email: u.email, fullName: u.fullName,
      role: u.role, devGroupId: u.devGroupId ? String(u.devGroupId) : '',
      password: '',
    })
    setError(null)
    setShowPassword(false)
    setOpen(true)
  }
  const openResetPassword = (u: UserDto) => {
    setResetTarget(u)
    setResetPassword('')
    setShowResetPassword(false)
    setResetError(null)
    setResetOpen(true)
  }

  const isManagedLeader = editing?.role === 'GROUP_LEAD'
  const needsGroup = form.role !== 'ADMIN'
  const missingGroup = needsGroup && !form.devGroupId
  const missingPassword = !editing && !form.password
  const canSubmit = form.username && form.email && form.fullName && !missingGroup && !missingPassword

  const handleSave = async () => {
    setError(null)
    const payload: Record<string, unknown> = {
      username: form.username,
      email: form.email,
      fullName: form.fullName,
      role: form.role,
      devGroupId: form.devGroupId ? Number(form.devGroupId) : null,
    }
    if (form.password) payload.password = form.password
    try {
      if (editing) await userApi.update(editing.id, payload)
      else await userApi.create(payload)
      setOpen(false)
      load()
    } catch (e: any) {
      setError(e.message || '保存失败')
    }
  }

  const toggleActive = async (u: UserDto) => {
    await userApi.setActive(u.id, !u.active)
    load()
  }

  const handleResetPassword = async () => {
    if (!resetTarget) return
    if (!resetPassword) {
      setResetError('请输入新密码')
      return
    }
    if (resetPassword.length < 6) {
      setResetError('新密码至少 6 位')
      return
    }
    setResetSaving(true)
    setResetError(null)
    try {
      await userApi.resetPassword(resetTarget.id, resetPassword)
      setResetOpen(false)
    } catch (e: any) {
      setResetError(e.message || '重置失败')
    } finally {
      setResetSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-sm">
            <UsersIcon className="h-5 w-5 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">用户管理</h1>
            <p className="text-sm text-gray-500 mt-0.5">管理系统账号、角色与所属开发组</p>
          </div>
        </div>
        <Button onClick={openCreate} className="shadow-sm">
          <Plus className="h-4 w-4 mr-1" />添加用户
        </Button>
      </div>

      <div className="flex gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <Input className="pl-9" placeholder="搜索用户名、邮箱、姓名..." value={q}
            onChange={e => { setQ(e.target.value); load(e.target.value) }} />
        </div>
      </div>

      <div className="data-table">
        <table className="w-full text-sm">
          <thead>
            <tr>
              <th>用户名</th>
              <th>姓名</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>开发组</th>
              <th>状态</th>
              <th>创建时间</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={8} className="text-center py-10 text-gray-400">加载中...</td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan={8} className="text-center py-10 text-gray-400">暂无用户</td></tr>
            ) : users.map(u => {
              const meta = roleMeta(u.role)
              return (
                <tr key={u.id} className={!u.active ? 'opacity-60' : ''}>
                  <td className="px-4 py-3 font-mono text-gray-700">{u.username}</td>
                  <td className="px-4 py-3 font-medium text-gray-900">{u.fullName}</td>
                  <td className="px-4 py-3 text-gray-500">{u.email}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex px-2 py-0.5 rounded-md text-xs font-medium ${meta.badge}`}>
                      {meta.label}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{u.devGroupName ?? '—'}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${u.active ? 'bg-green-100 text-green-700 ring-1 ring-green-200' : 'bg-gray-100 text-gray-500 ring-1 ring-gray-200'}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${u.active ? 'bg-green-500' : 'bg-gray-400'}`} />
                      {u.active ? '活跃' : '停用'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-400 text-xs">{formatDateTime(u.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" onClick={() => openEdit(u)} title="编辑">
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => openResetPassword(u)} title="重置密码">
                        <KeyRound className="h-3.5 w-3.5 text-blue-500" />
                      </Button>
                      <Button variant="ghost" size="icon" onClick={() => toggleActive(u)}
                        title={u.active ? '停用' : '启用'}>
                        {u.active
                          ? <UserX className="h-3.5 w-3.5 text-orange-500" />
                          : <UserCheck className="h-3.5 w-3.5 text-green-500" />
                        }
                      </Button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>{editing ? '编辑用户' : '添加用户'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>用户名 *</Label>
                <Input value={form.username} disabled={!!editing}
                  onChange={e => setForm(f => ({ ...f, username: e.target.value }))} />
              </div>
              <div className="space-y-1.5">
                <Label>姓名 *</Label>
                <Input value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label>邮箱 *</Label>
              <Input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
            </div>
            <div className="space-y-1.5">
              <Label>{editing ? '密码（留空则不修改）' : '密码 *'}</Label>
              <div className="relative">
                <Input
                  type={showPassword ? 'text' : 'password'}
                  value={form.password}
                  placeholder={editing ? '保持不变' : '至少 6 位'}
                  onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                />
                <button type="button" onClick={() => setShowPassword(s => !s)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600">
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>角色</Label>
                {isManagedLeader ? (
                  <div className="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                    组长
                    <p className="mt-1 text-xs text-amber-700">组长身份由“开发组管理”页面维护，这里只能修改基础资料和密码。</p>
                  </div>
                ) : (
                  <Select value={form.role} onValueChange={v => setForm(f => ({ ...f, role: v }))}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {EDITABLE_ROLES.map(r => <SelectItem key={r.value} value={r.value}>{r.label}</SelectItem>)}
                    </SelectContent>
                  </Select>
                )}
              </div>
              <div className="space-y-1.5">
                <Label>开发组{needsGroup && ' *'}</Label>
                {isManagedLeader ? (
                  <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
                    {editing?.devGroupName ?? '未分配'}
                  </div>
                ) : (
                  <Select value={form.devGroupId || 'NONE'} onValueChange={v => setForm(f => ({ ...f, devGroupId: v === 'NONE' ? '' : v }))}>
                    <SelectTrigger><SelectValue placeholder="不分配" /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="NONE">不分配</SelectItem>
                      {groups.map(g => <SelectItem key={g.id} value={String(g.id)}>{g.name}</SelectItem>)}
                    </SelectContent>
                  </Select>
                )}
                {missingGroup && <p className="text-xs text-red-500">非管理员必须选择开发组</p>}
              </div>
            </div>

            {error && (
              <div className="rounded-lg bg-red-50 border border-red-200 px-3 py-2 text-sm text-red-700">
                {error}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setOpen(false)}>取消</Button>
              <Button onClick={handleSave} disabled={!canSubmit}>
                {editing ? '保存' : '创建'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      <Dialog open={resetOpen} onOpenChange={setResetOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>重置密码</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 pt-2">
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700">
              为 <span className="font-semibold">{resetTarget?.fullName}</span>（{resetTarget?.username}）设置新的登录密码。
            </div>

            <div className="space-y-1.5">
              <Label>新密码</Label>
              <div className="relative">
                <Input
                  type={showResetPassword ? 'text' : 'password'}
                  value={resetPassword}
                  placeholder="至少 6 位"
                  onChange={e => setResetPassword(e.target.value)}
                />
                <button type="button" onClick={() => setShowResetPassword(s => !s)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600">
                  {showResetPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {resetError && (
              <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                {resetError}
              </div>
            )}

            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setResetOpen(false)}>取消</Button>
              <Button onClick={handleResetPassword} disabled={resetSaving}>
                {resetSaving ? '提交中...' : '确认重置'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
