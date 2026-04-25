import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getMemberById } from '../api/membersApi'
import { checkIn, getAttendanceByMember, deleteAttendance } from '../api/attendanceApi'
import { getNotesByMember, addNote, deleteNote } from '../api/notesApi'
import LoadingSpinner from '../components/common/LoadingSpinner'
import StatusBadge from '../components/common/StatusBadge'
import ConfirmDialog from '../components/common/ConfirmDialog'
import { useAuth } from '../context/AuthContext'

export default function MemberDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const [member, setMember] = useState(null)
  const [attendance, setAttendance] = useState([])
  const [notes, setNotes] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [newNote, setNewNote] = useState('')
  const [deleteAttTarget, setDeleteAttTarget] = useState(null)
  const [deleteNoteTarget, setDeleteNoteTarget] = useState(null)

  useEffect(() => { load() }, [id])

  async function load() {
    try {
      setLoading(true)
      const [mRes, aRes, nRes] = await Promise.all([
        getMemberById(id),
        getAttendanceByMember(id),
        getNotesByMember(id),
      ])
      setMember(mRes.data)
      setAttendance(aRes.data)
      setNotes(nRes.data)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  async function handleCheckIn() {
    try {
      await checkIn(id)
      const res = await getAttendanceByMember(id)
      setAttendance(res.data)
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleAddNote(e) {
    e.preventDefault()
    if (!newNote.trim()) return
    try {
      await addNote(id, newNote.trim())
      setNewNote('')
      const res = await getNotesByMember(id)
      setNotes(res.data)
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDeleteAttendance() {
    try {
      await deleteAttendance(deleteAttTarget)
      setDeleteAttTarget(null)
      const res = await getAttendanceByMember(id)
      setAttendance(res.data)
    } catch (e) {
      setError(e.message)
    }
  }

  async function handleDeleteNote() {
    try {
      await deleteNote(deleteNoteTarget)
      setDeleteNoteTarget(null)
      const res = await getNotesByMember(id)
      setNotes(res.data)
    } catch (e) {
      setError(e.message)
    }
  }

  if (loading) return <LoadingSpinner />
  if (!member) return <div className="page"><p>Member not found.</p></div>

  return (
    <div className="page">
      <button className="btn btn-secondary" style={{ marginBottom: '1rem' }} onClick={() => navigate('/members')}>
        ← Back
      </button>

      {error && <div className="error-banner">{error} <button onClick={() => setError(null)}>✕</button></div>}

      <div className="detail-card">
        <div className="detail-header">
          <div>
            <h1>{member.name}</h1>
            <p>{member.email} {member.phone && `· ${member.phone}`}</p>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.5rem' }}>
            <StatusBadge status={member.status} />
            {!isAdmin && (
              <button className="btn btn-primary" onClick={handleCheckIn}>Check In Now</button>
            )}
          </div>
        </div>
        <div className="detail-meta">
          <span>Joined: {member.joinDate || '—'}</span>
          <span>Plan: {member.membershipPlanName || '—'}</span>
          <span>Total check-ins: {attendance.length}</span>
          {member.nextPaymentDate && (
            <span>Next payment: <strong style={{ color: '#e94560' }}>{member.nextPaymentDate}</strong></span>
          )}
        </div>
      </div>

      {isAdmin && (
        <>
          <div className="card" style={{ marginBottom: '1rem' }}>
            <h2>Contact Information</h2>
            <dl className="profile-dl" style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', margin: 0 }}>
              <div>
                <dt>Email</dt>
                <dd>{member.email || '—'}</dd>
              </div>
              <div>
                <dt>Phone</dt>
                <dd>{member.phone || '—'}</dd>
              </div>
            </dl>
          </div>

          <div className="card" style={{ marginBottom: '1rem' }}>
            <h2>Payment Details</h2>
            <dl className="profile-dl" style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', margin: 0 }}>
              <div>
                <dt>Method</dt>
                <dd>{member.paymentMethod || '—'}</dd>
              </div>
              <div>
                <dt>Card Number</dt>
                <dd>{member.cardNumber ? `•••• •••• •••• ${member.cardNumber.slice(-4)}` : '—'}</dd>
              </div>
              <div>
                <dt>Expiry</dt>
                <dd>{member.cardExpiryDate || '—'}</dd>
              </div>
              <div>
                <dt>Billing Address</dt>
                <dd>
                  {member.streetAddress ? (
                    <>
                      {member.streetAddress}
                      {member.aptUnit && `, ${member.aptUnit}`}
                      {(member.city || member.state || member.zipCode) && (
                        <><br />{[member.city, member.state, member.zipCode].filter(Boolean).join(', ')}</>
                      )}
                    </>
                  ) : '—'}
                </dd>
              </div>
            </dl>
          </div>
        </>
      )}

      <div className="detail-grid">
        <section className="card">
          <h2>Attendance Log</h2>
          {attendance.length === 0 ? (
            <p style={{ color: '#888' }}>No check-ins yet.</p>
          ) : (
            <ul className="log-list">
              {attendance.map(a => (
                <li key={a.id}>
                  <span>{new Date(a.checkInTime).toLocaleString()}</span>
                  <button className="btn btn-sm btn-danger" onClick={() => setDeleteAttTarget(a.id)}>Remove</button>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="card">
          <h2>Notes</h2>
          <form onSubmit={handleAddNote} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
            <input
              style={{ flex: 1 }}
              placeholder="Add a note..."
              value={newNote}
              onChange={e => setNewNote(e.target.value)}
            />
            <button type="submit" className="btn btn-primary">Add</button>
          </form>
          {notes.length === 0 ? (
            <p style={{ color: '#888' }}>No notes yet.</p>
          ) : (
            <ul className="log-list">
              {notes.map(n => (
                <li key={n.id}>
                  <div>
                    <p style={{ margin: 0 }}>{n.content}</p>
                    <small style={{ color: '#888' }}>{new Date(n.createdAt).toLocaleString()}</small>
                  </div>
                  <button className="btn btn-sm btn-danger" onClick={() => setDeleteNoteTarget(n.id)}>Remove</button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {deleteAttTarget && (
        <ConfirmDialog
          message="Remove this attendance record?"
          onConfirm={handleDeleteAttendance}
          onCancel={() => setDeleteAttTarget(null)}
        />
      )}
      {deleteNoteTarget && (
        <ConfirmDialog
          message="Delete this note?"
          onConfirm={handleDeleteNote}
          onCancel={() => setDeleteNoteTarget(null)}
        />
      )}
    </div>
  )
}
