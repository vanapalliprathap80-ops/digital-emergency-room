import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Server, List, Activity, Share2,
  Zap, ShoppingCart, AlertTriangle
} from 'lucide-react'

const navItems = [
  { to: '/',          label: 'Overview',  icon: LayoutDashboard },
  { to: '/services',  label: 'Services',  icon: Server },
  { to: '/requests',  label: 'Requests',  icon: List },
  { to: '/events',    label: 'Events',    icon: Activity },
  { to: '/topology',  label: 'Topology',  icon: Share2 },
  { to: '/traffic',   label: 'Traffic',   icon: Zap },
  { to: '/orders',    label: 'Orders',    icon: ShoppingCart },
  { to: '/chaos',     label: 'Chaos',     icon: AlertTriangle },
]

export default function Sidebar() {
  return (
    <aside className="fixed inset-y-0 left-0 w-56 bg-surface border-r border-white/5 flex flex-col z-20">
      {/* Logo */}
      <div className="flex items-center gap-2 px-4 py-5 border-b border-white/5">
        <div className="flex items-center justify-center w-8 h-8 rounded bg-critical/20 border border-critical/30">
          <AlertTriangle size={16} className="text-critical" />
        </div>
        <div>
          <div className="text-sm font-bold text-text-main leading-tight">Digital ER</div>
          <div className="text-[10px] text-text-muted uppercase tracking-widest">Phase 2 Simulator</div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-2 py-4 space-y-0.5">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors ${
                isActive
                  ? 'bg-primary/10 text-primary border border-primary/20'
                  : 'text-text-muted hover:text-text-main hover:bg-white/4'
              }`
            }
          >
            <Icon size={16} />
            {label}
          </NavLink>
        ))}
      </nav>

      <div className="px-4 py-3 border-t border-white/5">
        <div className="text-[10px] text-text-muted uppercase tracking-wider">Production Simulator</div>
        <div className="text-[10px] text-text-muted mt-0.5">Phase 2 — Chaos Engine</div>
      </div>
    </aside>
  )
}
