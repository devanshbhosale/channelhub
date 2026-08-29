import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './AuthContext';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import ResellerDashboard from './pages/ResellerDashboard';
import Catalog from './pages/Catalog';
import Cart from './pages/Cart';
import Orders from './pages/Orders';
import OrderDetail from './pages/OrderDetail';
import AdminDashboard from './pages/AdminDashboard';
import AdminProducts from './pages/AdminProducts';
import AdminInventory from './pages/AdminInventory';
import AdminOrderQueue from './pages/AdminOrderQueue';
import AdminResellers from './pages/AdminResellers';

function Guard({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;
  }
  return <Layout>{children}</Layout>;
}

function PublicOnly({ children }) {
  const { user } = useAuth();
  if (user) return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/'} replace />;
  return children;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
          <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />

          <Route path="/" element={<Guard role="RESELLER"><ResellerDashboard /></Guard>} />
          <Route path="/catalog" element={<Guard role="RESELLER"><Catalog /></Guard>} />
          <Route path="/cart" element={<Guard role="RESELLER"><Cart /></Guard>} />
          <Route path="/orders" element={<Guard role="RESELLER"><Orders /></Guard>} />
          <Route path="/orders/:id" element={<Guard role="RESELLER"><OrderDetail /></Guard>} />

          <Route path="/admin" element={<Guard role="ADMIN"><AdminDashboard /></Guard>} />
          <Route path="/admin/products" element={<Guard role="ADMIN"><AdminProducts /></Guard>} />
          <Route path="/admin/inventory" element={<Guard role="ADMIN"><AdminInventory /></Guard>} />
          <Route path="/admin/queue" element={<Guard role="ADMIN"><AdminOrderQueue /></Guard>} />
          <Route path="/admin/resellers" element={<Guard role="ADMIN"><AdminResellers /></Guard>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
