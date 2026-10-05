import { Routes, Route } from 'react-router-dom'
import Sidebar from './components/Sidebar'
import OverviewPage  from './pages/OverviewPage'
import ServicesPage  from './pages/ServicesPage'
import RequestsPage  from './pages/RequestsPage'
import EventsPage    from './pages/EventsPage'
import TopologyPage  from './pages/TopologyPage'
import TrafficPage   from './pages/TrafficPage'
import OrdersPage    from './pages/OrdersPage'
import ChaosPage     from './pages/ChaosPage'
import InvestigationPage from './pages/InvestigationPage'

export default function App() {
  return (
    <div className="flex h-full min-h-screen bg-bg text-text-main">
      <Sidebar />
      <main className="ml-56 flex-1 p-6 overflow-auto">
        <Routes>
          <Route path="/"          element={<OverviewPage />} />
          <Route path="/services"  element={<ServicesPage />} />
          <Route path="/requests"  element={<RequestsPage />} />
          <Route path="/events"    element={<EventsPage />} />
          <Route path="/topology"  element={<TopologyPage />} />
          <Route path="/traffic"   element={<TrafficPage />} />
          <Route path="/orders"    element={<OrdersPage />} />
          <Route path="/chaos"     element={<ChaosPage />} />
          <Route path="/investigations/:incidentId" element={<InvestigationPage />} />
        </Routes>
      </main>
    </div>
  )
}
