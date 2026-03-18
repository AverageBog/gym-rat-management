import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMembers, createMember, updateMember, deleteMember } from '../api/membersApi'
import { getPlans } from '../api/plansApi'
import ConfirmDialog from '../components/common/ConfirmDialog'
import LoadingSpinner from '../components/common/LoadingSpinner'
import StatusBadge from '../components/common/StatusBadge'

const emptyForm = { name: '', email: '', phone: '', joinDate: '', status: 'ACTIVE', membershipPlanId: '' }

export default function MembersPage() {
  const [members, setMembers] = useState([])
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [search, setSearch] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([loadMembers(), loadPlans()])
  }, [])

  async function loadMembers(status) {
    try {
      setLoading(true)
      const res = await getMembers(status || undefined)
      setMembers(res.data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  async function loadPlans() {
    try {
      const res = await getPlans()
      setPlans(res.data)
    } catch {}
  }

  function handleFilterChange(val) {
    setStatusFilter(val)
    loadMembers(val || undefined)
  }

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setShowForm(true)
  }

  function openEdit(m) {
    setEditing(m.id)
    setForm({
      name: m.name,
      email: m.email,
      phone: m.phone || '',
      joinDate: m.joinDate || '',
      status: m.status || 'ACTIVE',
      membershipPlanId: m.membershipPlanId || '',
    })
    setShowForm(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const payload = { ...form, membershipPlanId: form.membershipPlanId ? Number(form.membershipPlanId) : null }
    try {
      if (editing) {
        await updateMember(editing, payload)
      } else {
        await createMember(payload)
      }
      setShowForm(false)
      loadMembers(statusFilter || undefined)
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDelete() {
    try {
      await deleteMember(deleteTarget)
      setDeleteTarget(null)
      loadMembers(statusFilter || undefined)
    } catch (e) {
      setError(e.message)
    }
  }

  const displayed = members.filter(m =>
    !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.email.toLowerCase().includes(search.toLowerCase())
  )

  if (loading && members.length === 0) return <LoadingSpinner />

  return (
    <div className="page">
      <div className="page-header">
        <h1>Members</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Member</button>
      </div>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}

      <div className="toolbar">
        <input
          className="search-input"
          placeholder="Search by name or email..."
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <select value={statusFilter} onChange={e => handleFilterChange(e.target.value)}>
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>

      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Phone</th>
            <th>Join Date</th>
            <th>Status</th>
            <th>Plan</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {displayed.map(m => (
            <tr key={m.id} className="clickable-row" onClick={() => navigate(`/members/${m.id}`)}>
              <td>{m.name}</td>
              <td>{m.email}</td>
              <td>{m.phone}</td>
              <td>{m.joinDate}</td>
              <td><StatusBadge status={m.status} /></td>
              <td>{m.membershipPlanName || '—'}</td>
              <td onClick={e => e.stopPropagation()}>
                <button className="btn btn-sm btn-secondary" onClick={() => openEdit(m)}>Edit</button>
                <button className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(m.id)}>Delete</button>
              </td>
            </tr>
          ))}
          {displayed.length === 0 && <tr><td colSpan={7} style={{ textAlign: 'center', color: '#888' }}>No members found.</td></tr>}
        </tbody>
      </table>

      {showForm && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>{editing ? 'Edit Member' : 'New Member'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Name</label>
                <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" required value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Phone</label>
                <input value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Join Date</label>
                <input type="date" value={form.joinDate} onChange={e => setForm({ ...form, joinDate: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Status</label>
                <select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="SUSPENDED">Suspended</option>
                </select>
              </div>
              <div className="form-group">
                <label>Membership Plan</label>
                <select value={form.membershipPlanId} onChange={e => setForm({ ...form, membershipPlanId: e.target.value })}>
                  <option value="">None</option>
                  {plans.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">{editing ? 'Save' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {deleteTarget && (
        <ConfirmDialog
          message="Delete this member? This cannot be undone."
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
