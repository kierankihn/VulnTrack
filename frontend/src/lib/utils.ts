import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit'
  })
}

export function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

export function formatRepoUrlLabel(repoUrl: string): string {
  try {
    const url = new URL(repoUrl)
    return url.pathname.length > 1 ? url.pathname.slice(1) : url.host
  } catch {
    return repoUrl
  }
}

export function formatJsonForDisplay(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

export const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-800 border-red-200',
  HIGH:     'bg-orange-100 text-orange-800 border-orange-200',
  MEDIUM:   'bg-yellow-100 text-yellow-800 border-yellow-200',
  LOW:      'bg-blue-100 text-blue-800 border-blue-200',
  NONE:     'bg-gray-100 text-gray-600 border-gray-200',
}

export const STATUS_COLORS: Record<string, string> = {
  OPEN:        'bg-blue-100 text-blue-800',
  IN_PROGRESS: 'bg-purple-100 text-purple-800',
  IN_REVIEW:   'bg-yellow-100 text-yellow-800',
  RESOLVED:    'bg-green-100 text-green-800',
  CLOSED:      'bg-gray-100 text-gray-600',
  WONT_FIX:    'bg-red-100 text-red-600',
}

export const STATUS_LABELS: Record<string, string> = {
  OPEN:        '待处理',
  IN_PROGRESS: '处理中',
  IN_REVIEW:   '审核中',
  RESOLVED:    '已修复',
  CLOSED:      '已关闭',
  WONT_FIX:    '不修复',
}

export const PRIORITY_COLORS: Record<string, string> = {
  CRITICAL: 'bg-red-100 text-red-800',
  HIGH:     'bg-orange-100 text-orange-800',
  MEDIUM:   'bg-yellow-100 text-yellow-800',
  LOW:      'bg-green-100 text-green-800',
}

export const PRIORITY_LABELS: Record<string, string> = {
  CRITICAL: '严重',
  HIGH:     '高危',
  MEDIUM:   '中危',
  LOW:      '低危',
}
