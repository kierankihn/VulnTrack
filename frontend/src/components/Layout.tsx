import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import {
  Shield, TicketCheck, Database, Users, Layers, Home, Building2, LogOut,
  Settings as SettingsIcon, KeyRound, ChevronUp,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth, hasAccess, PAGE_ACCESS } from '@/lib/auth'
import ChangePasswordDialog from './ChangePasswordDialog'

type PageKey = keyof typeof PAGE_ACCESS

const navItems: { label: string; icon: any; href: string; page: PageKey }[] = [
  { label: '总览',       icon: Home,        href: '/',            page: 'dashboard' },
  { label: 'CVE 数据库', icon: Database,    href: '/cves',        page: 'cves' },
  { label: '工单管理',   icon: TicketCheck, href: '/tickets',     page: 'tickets' },
  { label: '资产管理',   icon: Layers,      href: '/assets',      page: 'assets' },
  { label: '开发组',     icon: Building2,   href: '/dev-groups',  page: 'devGroups' },
  { label: '用户管理',   icon: Users,       href: '/users',       page: 'users' },
  { label: '系统设置',   icon: SettingsIcon,href: '/settings',    page: 'settings' },
]

const ROLE_LABELS: Record<string, string> = {
  ADMIN: '管理员',
  GROUP_LEAD: '组长',
  DEVELOPER: '开发者',
  TESTER: '测试员',
}

const ROLE_COLORS: Record<string, string> = {
  ADMIN:      'bg-red-100 text-red-600',
  GROUP_LEAD: 'bg-amber-100 text-amber-600',
  DEVELOPER:  'bg-blue-100 text-blue-600',
  TESTER:     'bg-emerald-100 text-emerald-600',
}

function Avatar({ name }: { name: string }) {
  const initials = name.trim().slice(0, 2).toUpperCase() || 'VT'
  return (
    <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 text-xs font-bold text-white">
      {initials}
    </div>
  )
}

export default function Layout({ children }: { children: React.ReactNode }) {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [changePwOpen, setChangePwOpen] = useState(false)

  const handleLogout = async () => {
    await logout()
    navigate('/login', { replace: true })
  }

  const items = navItems.filter(i => hasAccess(user?.role, i.page))

  return (
    <div className="flex min-h-screen bg-slate-100">
      <aside className="w-64 flex-shrink-0 p-3">
        <div className="sticky top-3 flex h-[calc(100vh-1.5rem)] flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">

          {/* Logo */}
          <div className="flex items-center gap-3 px-4 py-5">
            <div className="flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white">
              <Shield className="h-4 w-4" />
            </div>
            <div>
              <p className="text-sm font-semibold text-slate-900">VulnTrack</p>
              <p className="text-xs text-slate-400">漏洞追踪工作台</p>
            </div>
          </div>

          {/* Nav */}
          <nav className="flex-1 space-y-0.5 overflow-y-auto px-2 pb-2">
            <p className="mb-1 px-2 text-[10px] font-semibold uppercase tracking-widest text-slate-400">导航</p>
            {items.map(({ label, icon: Icon, href }) => {
              const active = href === '/' ? pathname === '/' : pathname.startsWith(href)
              return (
                <Link
                  key={href}
                  to={href}
                  className={cn(
                    'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors',
                    active
                      ? 'bg-blue-600 text-white'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
                  )}
                >
                  <Icon className={cn('h-4 w-4 flex-shrink-0', active ? 'text-blue-100' : 'text-slate-400')} />
                  {label}
                </Link>
              )
            })}
          </nav>

          {/* User */}
          <div className="border-t border-slate-100 p-2">
            {user && (
              <div className="relative">
                <button
                  onClick={() => setMenuOpen(v => !v)}
                  className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors hover:bg-slate-100"
                >
                  <Avatar name={user.fullName || user.username} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-slate-900">{user.fullName || user.username}</p>
                    <span className={cn('inline-flex rounded-md px-1.5 py-0.5 text-[10px] font-medium',
                      ROLE_COLORS[user.role] ?? 'bg-slate-100 text-slate-500')}>
                      {ROLE_LABELS[user.role] ?? user.role}
                    </span>
                  </div>
                  <ChevronUp className={cn('h-4 w-4 flex-shrink-0 text-slate-400 transition-transform', menuOpen ? 'rotate-180' : '')} />
                </button>

                {menuOpen && (
                  <>
                    <div className="fixed inset-0 z-40" onClick={() => setMenuOpen(false)} />
                    <div className="absolute bottom-full left-0 right-0 z-50 mb-1 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
                      {user.devGroupName && (
                        <div className="border-b border-slate-100 px-3 py-2">
                          <p className="truncate text-xs text-slate-400">{user.devGroupName}</p>
                        </div>
                      )}
                      <button
                        onClick={() => { setMenuOpen(false); setChangePwOpen(true) }}
                        className="flex w-full items-center gap-2 px-3 py-2.5 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        <KeyRound className="h-4 w-4 text-slate-400" /> 修改密码
                      </button>
                      <button
                        onClick={() => { setMenuOpen(false); handleLogout() }}
                        className="flex w-full items-center gap-2 px-3 py-2.5 text-sm text-red-600 hover:bg-red-50"
                      >
                        <LogOut className="h-4 w-4" /> 退出登录
                      </button>
                    </div>
                  </>
                )}
              </div>
            )}
          </div>
        </div>
      </aside>

      <main className="flex-1 overflow-hidden p-3 pl-0">
        <div className="h-[calc(100vh-1.5rem)] overflow-y-auto rounded-2xl border border-slate-200 bg-white shadow-sm">
          <div className="mx-auto max-w-[1400px] p-8">
            {children}
          </div>
        </div>
      </main>

      <ChangePasswordDialog open={changePwOpen} onOpenChange={setChangePwOpen} />
    </div>
  )
}
