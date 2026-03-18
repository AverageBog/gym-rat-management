import { useEffect, useState } from 'react'
import { getMerchandise, createMerchandise, updateMerchandise, adjustQuantity, deleteMerchandise } from '../api/merchandiseApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ConfirmDialog from '../components/common/ConfirmDialog'

const emptyForm = { name: '', quantity: '', price: '', description: '' }

export default function MerchandisePage() {
  const [items, setItems] = useState([])
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
      const res = await getMerchandise()
      setItems(res.data)
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

  function openEdit(item) {
    setEditing(item.id)
    setForm({ name: item.name, quantity: item.quantity, price: item.price, description: item.description || '' })
    setShowForm(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const payload = { ...form, quantity: Number(form.quantity), price: Number(form.price) }
    try {
      if (editing) {
        await updateMerchandise(editing, payload)
      } else {
        await createMerchandise(payload)
      }
      setShowForm(false)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleAdjust(id, delta) {
    try {
      const res = await adjustQuantity(id, delta)
      setItems(items.map(i => i.id === id ? res.data : i))
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDelete() {
    try {
      await deleteMerchandise(deleteTarget)
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
        <h1>Merchandise Inventory</h1>
        <button className="btn btn-primary" onClick={openCreate}>+ New Item</button>
      </div>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}

      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Quantity</th>
            <th>Price</th>
            <th>Description</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {items.map(item => (
            <tr key={item.id}>
              <td>{item.name}</td>
              <td>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <button className="btn btn-sm btn-secondary" onClick={() => handleAdjust(item.id, -1)}>−</button>
                  <span style={{ minWidth: '2rem', textAlign: 'center' }}>{item.quantity}</span>
                  <button className="btn btn-sm btn-secondary" onClick={() => handleAdjust(item.id, 1)}>+</button>
                </div>
              </td>
              <td>${item.price}</td>
              <td>{item.description}</td>
              <td>
                <button className="btn btn-sm btn-secondary" onClick={() => openEdit(item)}>Edit</button>
                <button className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(item.id)}>Delete</button>
              </td>
            </tr>
          ))}
          {items.length === 0 && <tr><td colSpan={5} style={{ textAlign: 'center', color: '#888' }}>No items yet.</td></tr>}
        </tbody>
      </table>

      {showForm && (
        <div className="modal-overlay">
          <div className="modal">
            <h2>{editing ? 'Edit Item' : 'New Item'}</h2>
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Name</label>
                <input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Quantity</label>
                <input type="number" required min="0" value={form.quantity} onChange={e => setForm({ ...form, quantity: e.target.value })} />
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
          message="Delete this item?"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
