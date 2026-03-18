import { useEffect, useState } from 'react'
import { getPlans, createPlan, updatePlan, deletePlan } from '../api/plansApi'
import ConfirmDialog from '../components/common/ConfirmDialog'
import LoadingSpinner from '../components/common/LoadingSpinner'

const emptyForm = { name: '', durationMonths: '', price: '', description: '' }

export default function PlansPage() {
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [deleteTarget, setDeleteTarget] = useState(null)

  useEffect(() => { load() }, [])

  async function load() {
    try {
      setLoading(true)
      const res = await getPlans()
      setPlans(res.data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setShowForm(true)
  }

  function openEdit(plan) {
    setEditing(plan.id)
    setForm({ name: plan.name, durationMonths: plan.durationMonths, price: plan.price, description: plan.description || '' })
    setShowForm(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const payload = { ...form, durationMonths: Number(form.durationMonths), price: Number(form.price) }
    try {
      if (editing) {
        await updatePlan(editing, payload)
      } else {
        await createPlan(payload)
      }
      setShowForm(false)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDelete() {
    try {
      await deletePlan(deleteTarget)
      setDeleteTarget(null)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  if (loading) return <LoadingSpinner />

  return (
    <div className="page">
      <div className="page-header">
        <h1>Membership Plans</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Plan</button>
      </div>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}

      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Duration</th>
            <th>Price</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {plans.map(plan => (
            <tr key={plan.id}>
              <td>{plan.name}</td>
              <td>{plan.durationMonths} month{plan.durationMonths !== 1 ? 's' : ''}</td>
              <td>${plan.price}</td>
              <td>{plan.description}</td>
              <td>
                <button className="btn btn-sm btn-secondary" onClick={() => openEdit(plan)}>Edit</button>
                <button className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(plan.id)}>Delete</button>
              </td>
            </tr>
          ))}
          {plans.length === 0 && <tr><td colSpan={5} style={{ textAlign: 'center', color: '#888' }}>No plans yet.</td></tr>}
        </tbody>
      </table>

      {showForm && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>{editing ? 'Edit Plan' : 'New Plan'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Name</label>
                <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Duration (months)</label>
                <input type="number" required min="1" value={form.durationMonths} onChange={e => setForm({ ...form, durationMonths: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Price ($)</label>
                <input type="number" required min="0" step="0.01" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Description</label>
                <input value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
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
          message="Delete this plan? Members assigned to it will lose their plan association."
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
