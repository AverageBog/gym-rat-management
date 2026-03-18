import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Navbar from './components/layout/Navbar'
import DashboardPage from './pages/DashboardPage'
import MembersPage from './pages/MembersPage'
import MemberDetailPage from './pages/MemberDetailPage'
import PlansPage from './pages/PlansPage'
import AttendancePage from './pages/AttendancePage'
import MerchandisePage from './pages/MerchandisePage'

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/members" element={<MembersPage />} />
        <Route path="/members/:id" element={<MemberDetailPage />} />
        <Route path="/plans" element={<PlansPage />} />
        <Route path="/attendance" element={<AttendancePage />} />
        <Route path="/merchandise" element={<MerchandisePage />} />
      </Routes>
    </BrowserRouter>
  )
}
