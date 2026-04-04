import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { createAdmin } from '../../api/authApi'
import './Navbar.css'

const adminLinks = [
  { to: '/', label: 'Dashboard', exact: true },
  { to: '/members', label: 'Members' },
  { to: '/plans', label: 'Plans' },
  { to: '/attendance', label: 'Attendance' },
  { to: '/merchandise', label: 'Merchandise' },
]

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [showAdminModal, setShowAdminModal] = useState(false)
  const [adminForm, setAdminForm] = useState({ email: '', password: '' })
  const [adminError, setAdminError] = useState(null)
  const [adminSuccess, setAdminSuccess] = useState(false)
  const [saving, setSaving] = useState(false)

  const links = user?.role === 'ADMIN' ? adminLinks : []

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  async function handleCreateAdmin(e) {
    e.preventDefault()
    setSaving(true)
    setAdminError(null)
    try {
      await createAdmin(adminForm.email, adminForm.password)
      setAdminSuccess(true)
      setAdminForm({ email: '', password: '' })
      setTimeout(() => { setAdminSuccess(false); setShowAdminModal(false) }, 2000)
    } catch (err) {
      setAdminError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <nav className="navbar">
        <span className="navbar-brand">GymRat CRM</span>

        <ul className="navbar-links">
          {links.map(({ to, label, exact }) => (
            <li key={to}>
              <NavLink to={to} end={exact} className={({ isActive }) => isActive ? 'active' : ''}>
                {label}
              </NavLink>
            </li>
          ))}
        </ul>

        <div className="navbar-user">
          <span className="navbar-username" title={user?.email}>
            {user?.role === 'ADMIN' ? '🛡 Admin' : `👤 ${user?.name || user?.email}`}
          </span>
          {user?.role === 'ADMIN' && (
            <button className="btn btn-sm btn-secondary" onClick={() => setShowAdminModal(true)}>
              + Admin
            </button>
          )}
          <button className="btn btn-sm btn-secondary" onClick={handleLogout}>
            Sign Out
          </button>
        </div>
      </nav>

      {showAdminModal && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>Create Admin Account</h2>
            {adminSuccess ? (
              <p style={{ color: '#22c55e' }}>Admin account created successfully!</p>
            ) : (
              <form onSubmit={handleCreateAdmin}>
                {adminError && (
                  <div className="error-banner" style={{ marginBottom: '1rem' }}>
                    {adminError}
                    <button type="button" onClick={() => setAdminError(null)}>✕</button>
                  </div>
                )}
                <div className="form-group">
                  <label>Email</label>
                  <input
                    type="email"
                    required
                    value={adminForm.email}
                    onChange={e => setAdminForm({ ...adminForm, email: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label>Password (min 8 characters)</label>
                  <input
                    type="password"
                    required
                    minLength={8}
                    value={adminForm.password}
                    onChange={e => setAdminForm({ ...adminForm, password: e.target.value })}
                  />
                </div>
                <div className="modal-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => setShowAdminModal(false)}>Cancel</button>
                  <button type="submit" className="btn btn-primary" disabled={saving}>Create</button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </>
  )
}
