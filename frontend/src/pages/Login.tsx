import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Shield, Lock, User, AlertCircle } from 'lucide-react'
import { useAuth } from '@/lib/auth'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username || !password) return
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err: any) {
      setError(err.message || '登录失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-blue-600 shadow-lg mb-4">
            <Shield className="h-8 w-8 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">VulnTrack</h1>
          <p className="text-slate-400 text-sm mt-1">漏洞追踪与工单管理系统</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-2xl p-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-6">登录账户</h2>

          {error && (
            <div className="flex items-center gap-2 rounded-lg bg-red-50 border border-red-200 px-4 py-3 mb-5 text-sm text-red-700">
              <AlertCircle className="h-4 w-4 flex-shrink-0" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="username" className="text-gray-700">用户名 / 邮箱</Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  id="username"
                  type="text"
                  placeholder="请输入用户名或邮箱"
                  className="pl-9 h-11"
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  autoComplete="username"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-gray-700">密码</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  id="password"
                  type="password"
                  placeholder="请输入密码"
                  className="pl-9 h-11"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>
            </div>

            <Button
              type="submit"
              className="w-full h-11 text-base font-medium bg-blue-600 hover:bg-blue-700"
              disabled={loading}
            >
              {loading ? '登录中...' : '登录'}
            </Button>
          </form>
        </div>

        <p className="text-center text-slate-500 text-xs mt-6">
          漏洞追踪系统 v1.0 · 面向对象程序设计导论
        </p>
      </div>
    </div>
  )
}
