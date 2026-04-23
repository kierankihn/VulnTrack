import { useEffect, useState } from 'react'
import { settingsApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Settings as SettingsIcon, Key, Save, Eye, EyeOff, CheckCircle2, AlertCircle } from 'lucide-react'

export default function Settings() {
  const [loaded, setLoaded] = useState(false)
  const [configured, setConfigured] = useState(false)
  const [masked, setMasked] = useState('')
  const [newValue, setNewValue] = useState('')
  const [show, setShow] = useState(false)
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const load = () => {
    settingsApi.getNvdKey().then(r => {
      setConfigured(r.configured)
      setMasked(r.maskedValue)
      setLoaded(true)
    })
  }

  useEffect(() => { load() }, [])

  const save = async () => {
    setSaving(true)
    setMsg(null)
    try {
      await settingsApi.setNvdKey(newValue)
      setMsg({ type: 'ok', text: 'NVD API Key 已保存' })
      setNewValue('')
      load()
    } catch (e: any) {
      setMsg({ type: 'err', text: e.message || '保存失败' })
    } finally {
      setSaving(false)
    }
  }

  const clear = async () => {
    if (!confirm('清除当前 NVD API Key？后续同步将匿名访问，可能触发速率限制。')) return
    setSaving(true)
    try {
      await settingsApi.setNvdKey('')
      setMsg({ type: 'ok', text: 'NVD API Key 已清除' })
      load()
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-slate-600 to-slate-800 flex items-center justify-center shadow-sm">
          <SettingsIcon className="h-5 w-5 text-white" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">系统设置</h1>
          <p className="text-sm text-gray-500 mt-0.5">管理外部集成与全局配置</p>
        </div>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Key className="h-4 w-4 text-amber-500" />
            NVD API Key
          </CardTitle>
          <p className="text-xs text-gray-500 mt-1">
            用于从 NVD 同步 CVE 数据。未配置时将以匿名身份请求，可能触发限流。
            <a href="https://nvd.nist.gov/developers/request-an-api-key" target="_blank" rel="noreferrer"
               className="text-blue-600 hover:underline ml-1">申请 Key →</a>
          </p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex items-center gap-2 text-sm">
            {loaded && (configured ? (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-green-50 text-green-700 ring-1 ring-green-200 text-xs font-medium">
                <CheckCircle2 className="h-3.5 w-3.5" /> 已配置
                <span className="ml-1 font-mono text-green-600">{masked}</span>
              </span>
            ) : (
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-amber-50 text-amber-700 ring-1 ring-amber-200 text-xs font-medium">
                <AlertCircle className="h-3.5 w-3.5" /> 未配置
              </span>
            ))}
          </div>

          <div className="space-y-1.5">
            <Label>{configured ? '更新为新的 Key' : '设置 Key'}</Label>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Input
                  type={show ? 'text' : 'password'}
                  placeholder="粘贴 NVD API Key"
                  value={newValue}
                  onChange={e => setNewValue(e.target.value)}
                  className="font-mono pr-9"
                />
                <button type="button" onClick={() => setShow(s => !s)}
                  className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600">
                  {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              <Button onClick={save} disabled={!newValue || saving}>
                <Save className="h-4 w-4 mr-1" />保存
              </Button>
              {configured && (
                <Button variant="outline" onClick={clear} disabled={saving}>清除</Button>
              )}
            </div>
          </div>

          {msg && (
            <div className={`rounded-lg px-3 py-2 text-sm ${msg.type === 'ok'
              ? 'bg-green-50 text-green-700 border border-green-200'
              : 'bg-red-50 text-red-700 border border-red-200'}`}>
              {msg.text}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
