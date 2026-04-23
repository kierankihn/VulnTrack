import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth, hasAccess, PAGE_ACCESS } from '@/lib/auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import CveList from './pages/CveList'
import CveDetail from './pages/CveDetail'
import TicketList from './pages/TicketList'
import TicketDetail from './pages/TicketDetail'
import TicketCreate from './pages/TicketCreate'
import Assets from './pages/Assets'
import DevGroups from './pages/DevGroups'
import Users from './pages/Users'
import Settings from './pages/Settings'
import Forbidden from './pages/Forbidden'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center">
      <div className="text-slate-400 text-sm">加载中...</div>
    </div>
  )
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function RoleRoute({ page, children }: { page: keyof typeof PAGE_ACCESS; children: React.ReactNode }) {
  const { user } = useAuth()
  if (!hasAccess(user?.role, page)) return <Forbidden />
  return <>{children}</>
}

function AppRoutes() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/*" element={
        <PrivateRoute>
          <Layout>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/cves" element={<CveList />} />
              <Route path="/cves/:id" element={<CveDetail />} />
              <Route path="/tickets" element={<TicketList />} />
              <Route path="/tickets/new" element={<RoleRoute page="ticketCreate"><TicketCreate /></RoleRoute>} />
              <Route path="/tickets/:id" element={<TicketDetail />} />
              <Route path="/assets" element={<RoleRoute page="assets"><Assets /></RoleRoute>} />
              <Route path="/dev-groups" element={<RoleRoute page="devGroups"><DevGroups /></RoleRoute>} />
              <Route path="/users" element={<RoleRoute page="users"><Users /></RoleRoute>} />
              <Route path="/settings" element={<RoleRoute page="settings"><Settings /></RoleRoute>} />
            </Routes>
          </Layout>
        </PrivateRoute>
      } />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
