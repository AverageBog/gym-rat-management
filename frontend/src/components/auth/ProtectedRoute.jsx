import { Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

/**
 * Wraps routes that require authentication.
 * adminOnly=true  → only ADMIN may access; members are redirected to /profile.
 * memberOnly=true → only MEMBER may access; admins are redirected to /.
 */
export default function ProtectedRoute({ children, adminOnly = false, memberOnly = false }) {
  const { user } = useAuth()

  if (!user) return <Navigate to="/login" replace />
  if (adminOnly && user.role !== 'ADMIN') return <Navigate to="/profile" replace />
  if (memberOnly && user.role !== 'MEMBER') return <Navigate to="/" replace />

  return children
}
