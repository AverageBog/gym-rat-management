import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import ProtectedRoute from './components/auth/ProtectedRoute'
import Navbar from './components/layout/Navbar'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import MembersPage from './pages/MembersPage'
import MemberDetailPage from './pages/MemberDetailPage'
import PlansPage from './pages/PlansPage'
import AttendancePage from './pages/AttendancePage'
import MerchandisePage from './pages/MerchandisePage'
import MyProfilePage from './pages/MyProfilePage'

function AppRoutes() {
  const { user } = useAuth()

  return (
    <>
      {user && <Navbar />}
      <Routes>
        <Route path="/login" element={user ? <Navigate to={user.role === 'MEMBER' ? '/profile' : '/'} replace /> : <LoginPage />} />

        <Route path="/" element={<ProtectedRoute adminOnly><DashboardPage /></ProtectedRoute>} />
        <Route path="/members" element={<ProtectedRoute adminOnly><MembersPage /></ProtectedRoute>} />
        <Route path="/members/:id" element={<ProtectedRoute adminOnly><MemberDetailPage /></ProtectedRoute>} />
        <Route path="/plans" element={<ProtectedRoute adminOnly><PlansPage /></ProtectedRoute>} />
        <Route path="/attendance" element={<ProtectedRoute adminOnly><AttendancePage /></ProtectedRoute>} />
        <Route path="/merchandise" element={<ProtectedRoute adminOnly><MerchandisePage /></ProtectedRoute>} />

        <Route path="/profile" element={<ProtectedRoute memberOnly><MyProfilePage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to={user ? (user.role === 'MEMBER' ? '/profile' : '/') : '/login'} replace />} />
      </Routes>
    </>
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
