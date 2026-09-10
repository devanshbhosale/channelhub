import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { useCart } from '../cart';

const TITLES = {
  '/': 'Dashboard',
  '/catalog': 'Catalog',
  '/cart': 'Cart',
  '/orders': 'Orders',
  '/admin': 'Admin Dashboard',
  '/admin/products': 'Products',
  '/admin/inventory': 'Inventory',
  '/admin/queue': 'Order Queue',
  '/admin/resellers': 'Resellers',
};

const RESELLER_NAV = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/catalog', label: 'Catalog' },
  { to: '/cart', label: 'Cart', cart: true },
  { to: '/orders', label: 'My Orders' },
];

const ADMIN_NAV = [
  { to: '/admin', label: 'Dashboard', end: true },
  { to: '/admin/queue', label: 'Order Queue' },
  { to: '/admin/products', label: 'Products' },
  { to: '/admin/inventory', label: 'Inventory' },
  { to: '/admin/resellers', label: 'Resellers' },
];

function initials(user) {
  const src = (user && (user.fullName || user.email)) || '?';
  const words = src.trim().split(/\s+/);
  const out = words.length > 1 ? words[0][0] + words[words.length - 1][0] : src.slice(0, 2);
  return out.toUpperCase();
}

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const loc = useLocation();
  const cart = useCart();

  const isAdmin = user && user.role === 'ADMIN';
  const nav = isAdmin ? ADMIN_NAV : RESELLER_NAV;
  const cartCount = cart.reduce((s, i) => s + i.qty, 0);
  const title = TITLES[loc.pathname] || (/^\/orders\/\d+$/.test(loc.pathname) ? 'Order Detail' : 'ChannelHub');

  const [sideClosed, setSideClosed] = useState(
    () => localStorage.getItem('ch-sidebar-closed') === '1'
  );
  const toggleSidebar = () =>
    setSideClosed((closed) => {
      localStorage.setItem('ch-sidebar-closed', closed ? '0' : '1');
      return !closed;
    });

  const demoText = isAdmin
    ? 'Demo mode — admin actions are disabled. Browse and inspect freely; write actions are blocked.'
    : 'Demo mode — you are using the sample reseller account. Ordering works normally.';

  return (
    <div className={'app' + (sideClosed ? ' side-closed' : '')}>
      <aside className="sidebar" id="app-sidebar">
        <div className="logo">
          <span className="logo-mark" />
          ChannelHub
        </div>
        <nav className="nav">
          <div className="nav-group">{isAdmin ? 'Administration' : 'Ordering'}</div>
          {nav.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.end}
              className={({ isActive }) => 'nav-link' + (isActive ? ' active' : '')}
            >
              <span>{n.label}</span>
              {n.cart && cartCount > 0 && <span className="nav-cart mono">{cartCount}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="side-foot">
          <div className="side-user">
            <div className="avatar">{initials(user)}</div>
            <div style={{ minWidth: 0 }}>
              <div className="side-name">{user.fullName || user.email}</div>
              <div className="side-email">{user.email}</div>
            </div>
          </div>
          <button className="side-logout" onClick={logout}>Sign out</button>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="topbar-left">
            <button
              type="button"
              className="side-toggle"
              onClick={toggleSidebar}
              aria-expanded={!sideClosed}
              aria-controls="app-sidebar"
              title={sideClosed ? 'Open sidebar' : 'Close sidebar'}
            >
              <svg width="15" height="15" viewBox="0 0 16 16" aria-hidden="true">
                <rect
                  x="1.5" y="2.5" width="13" height="11" rx="1.6"
                  fill="none" stroke="currentColor" strokeWidth="1.3"
                />
                <rect x="3" y="4" width="2.6" height="8" rx="0.8" fill="currentColor" />
              </svg>
            </button>
            <h1>{title}</h1>
          </div>
          <div className="topbar-right">
            {user.isDemo && <span className="chip chip-amber">Demo</span>}
            <span className={'role-chip' + (isAdmin ? ' role-admin' : '')}>{user.role}</span>
            <div className="avatar avatar-light">{initials(user)}</div>
          </div>
        </header>

        {user.isDemo && <div className="demo-banner">{demoText}</div>}

        <main className="content">{children}</main>
      </div>
    </div>
  );
}
