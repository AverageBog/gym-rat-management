import { useEffect, useState } from 'react'
import { getAllAttendance, deleteAttendance } from '../api/attendanceApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ConfirmDialog from '../components/common/ConfirmDialog'

export default function AttendancePage() {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)

  useEffect(() => { load() }, [])

  async function load(s, e) {
    try {
      setLoading(true)
      const res = await getAllAttendance(s || undefined, e || undefined)
      setRecords(res.data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function handleFilter(e) {
    e.preventDefault()
    const s = start ? start + 'T00:00:00' : undefined
    const en = end ? end + 'T23:59:59' : undefined
    load(s, en)
  }

  async function handleDelete() {
    try {
      await deleteAttendance(deleteTarget)
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
        <h1>Attendance Log</h1>
      </div>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}

      <form className="toolbar" onSubmit={handleFilter}>
        <label>From <input type="date" value={start} onChange={e => setStart(e.target.value)} /></label>
        <label>To <input type="date" value={end} onChange={e => setEnd(e.target.value)} /></label>
        <button type="submit" className="btn btn-primary">Filter</button>
        <button type="button" className="btn btn-secondary" onClick={() => { setStart(''); setEnd(''); load() }}>Clear</button>
      </form>

      <table className="table">
        <thead>
          <tr>
            <th>Member</th>
            <th>Check-In Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {records.map(r => (
            <tr key={r.id}>
              <td>{r.memberName}</td>
              <td>{new Date(r.checkInTime).toLocaleString()}</td>
              <td>
                <button className="btn btn-sm btn-danger" onClick={() => setDeleteTarget(r.id)}>Remove</button>
              </td>
            </tr>
          ))}
          {records.length === 0 && <tr><td colSpan={3} style={{ textAlign: 'center', color: '#888' }}>No records found.</td></tr>}
        </tbody>
      </table>

      {deleteTarget && (
        <ConfirmDialog
          message="Remove this attendance record?"
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}
