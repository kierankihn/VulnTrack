import { Link } from 'react-router-dom'
import { ShieldAlert, ArrowLeft } from 'lucide-react'
import { Button } from '@/components/ui/button'

export default function Forbidden() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] text-center">
      <div className="w-16 h-16 rounded-2xl bg-red-50 text-red-500 flex items-center justify-center mb-4">
        <ShieldAlert className="h-8 w-8" />
      </div>
      <h1 className="text-2xl font-bold text-gray-900 mb-1">无权访问</h1>
      <p className="text-sm text-gray-500 mb-6">当前角色没有访问此页面的权限。</p>
      <Button asChild variant="outline">
        <Link to="/"><ArrowLeft className="h-4 w-4 mr-1" />返回总览</Link>
      </Button>
    </div>
  )
}
