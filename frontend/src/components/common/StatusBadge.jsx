const colors = {
  ACTIVE: '#22c55e',
  INACTIVE: '#94a3b8',
  SUSPENDED: '#ef4444',
}

export default function StatusBadge({ status }) {
  const color = colors[status] || '#94a3b8'
  return (
    <span style={{
      display: 'inline-block',
      padding: '2px 10px',
      borderRadius: '12px',
      fontSize: '0.75rem',
      fontWeight: 600,
      background: color + '22',
      color,
      border: `1px solid ${color}55`,
    }}>
      {status}
    </span>
  )
}
