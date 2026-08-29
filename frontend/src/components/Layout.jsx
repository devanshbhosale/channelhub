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

  const demoText = isAdmin
    ? 'Demo mode — admin actions are disabled. Browse and inspect freely; write actions are blocked.'
    : 'Demo mode — you are using the sample reseller account. Ordering works normally.';

  return (
    <div className="app">
      <aside className="sidebar">
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
          <h1>{title}</h1>
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
