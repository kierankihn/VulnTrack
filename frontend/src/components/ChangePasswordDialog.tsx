import { useEffect, useState } from 'react'
import { authApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

interface ChangePasswordDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export default function ChangePasswordDialog({ open, onOpenChange }: ChangePasswordDialogProps) {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSubmitting(false)
      setError(null)
      setSuccess(null)
    }
  }, [open])

  const handleSubmit = async () => {
    setError(null)
    setSuccess(null)

    if (!currentPassword || !newPassword || !confirmPassword) {
      setError('请完整填写所有密码字段')
      return
    }
    if (newPassword.length < 6) {
      setError('新密码至少 6 位')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('两次输入的新密码不一致')
      return
    }

    setSubmitting(true)
    try {
      await authApi.changePassword(currentPassword, newPassword)
      setSuccess('密码修改成功')
      window.setTimeout(() => onOpenChange(false), 500)
    } catch (e: any) {
      setError(e.message || '密码修改失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>修改密码</DialogTitle>
        </DialogHeader>
        <div className="space-y-4 pt-2">
          <div className="space-y-1.5">
            <Label>当前密码</Label>
            <Input
              type="password"
              value={currentPassword}
              onChange={e => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>

          <div className="space-y-1.5">
            <Label>新密码</Label>
            <Input
              type="password"
              value={newPassword}
              onChange={e => setNewPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>

          <div className="space-y-1.5">
            <Label>确认新密码</Label>
            <Input
              type="password"
              value={confirmPassword}
              onChange={e => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>

          {error && (
            <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          {success && (
            <div className="rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-sm text-green-700">
              {success}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => onOpenChange(false)}>取消</Button>
            <Button onClick={handleSubmit} disabled={submitting}>
              {submitting ? '提交中...' : '确认修改'}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
