import { NavLink } from 'react-router-dom'
import './Navbar.css'

const links = [
  { to: '/', label: 'Dashboard', exact: true },
  { to: '/members', label: 'Members' },
  { to: '/plans', label: 'Plans' },
  { to: '/attendance', label: 'Attendance' },
  { to: '/merchandise', label: 'Merchandise' },
]

export default function Navbar() {
  return (
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
    </nav>
  )
}
