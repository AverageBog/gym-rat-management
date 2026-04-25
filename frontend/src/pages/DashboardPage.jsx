import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMembers } from '../api/membersApi'
import { getAllAttendance } from '../api/attendanceApi'
import { getMerchandise } from '../api/merchandiseApi'
import LoadingSpinner from '../components/common/LoadingSpinner'

export default function DashboardPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    async function load() {
      try {
        const [membersRes, activeRes, attendanceRes, mercRes] = await Promise.all([
          getMembers(),
          getMembers('ACTIVE'),
          getAllAttendance(),
          getMerchandise(),
        ])

        const today = new Date().toDateString()
        const todayCheckIns = attendanceRes.data.filter(a =>
          new Date(a.checkInTime).toDateString() === today
        ).length

        const lowStock = mercRes.data.filter(i => i.quantity <= 5).length

        setStats({
          totalMembers: membersRes.data.length,
          activeMembers: activeRes.data.length,
          todayCheckIns,
          lowStock,
          recentCheckIns: attendanceRes.data.slice(0, 5),
        })
      } catch (e) {
        console.error(e)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) return <LoadingSpinner />
  if (!stats) return (
    <div className="page">
      <div className="error-banner">Failed to load dashboard data. Please refresh.</div>
    </div>
  )

  return (
    <div className="page">
      <h1>Dashboard</h1>

      <div className="stats-grid">
        <div className="stat-card" onClick={() => navigate('/members')}>
          <div className="stat-value">{stats.totalMembers}</div>
          <div className="stat-label">Total Members</div>
        </div>
        <div className="stat-card" onClick={() => navigate('/members?status=ACTIVE')}>
          <div className="stat-value" style={{ color: '#22c55e' }}>{stats.activeMembers}</div>
          <div className="stat-label">Active Members</div>
        </div>
        <div className="stat-card" onClick={() => navigate('/attendance')}>
          <div className="stat-value" style={{ color: '#3b82f6' }}>{stats.todayCheckIns}</div>
          <div className="stat-label">Check-ins Today</div>
        </div>
        <div className="stat-card" onClick={() => navigate('/merchandise')}>
          <div className="stat-value" style={{ color: stats.lowStock > 0 ? '#ef4444' : '#22c55e' }}>{stats.lowStock}</div>
          <div className="stat-label">Low Stock Items</div>
        </div>
      </div>

      <div className="card" style={{ marginTop: '2rem' }}>
        <h2>Recent Check-ins</h2>
        {stats.recentCheckIns.length === 0 ? (
          <p style={{ color: '#888' }}>No check-ins yet.</p>
        ) : (
          <ul className="log-list">
            {stats.recentCheckIns.map(a => (
              <li key={a.id}>
                <span
                  style={{ cursor: 'pointer', color: '#3b82f6' }}
                  onClick={() => navigate(`/members/${a.memberId}`)}
                >
                  {a.memberName}
                </span>
                <span style={{ color: '#888', fontSize: '0.85rem' }}>
                  {new Date(a.checkInTime).toLocaleString()}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
