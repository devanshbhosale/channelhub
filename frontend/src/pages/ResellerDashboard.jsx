import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { apiError } from '../api';
import { useAuth } from '../AuthContext';
import { fmtINR, fmtDate, StatusChip } from '../format';

export default function ResellerDashboard() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [acct, setAcct] = useState(null);
  const [orders, setOrders] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    api.get('/account')
      .then((r) => setAcct(r.data))
      .catch((e) => setErr(apiError(e)));
    api.get('/orders')
      .then((r) => setOrders(Array.isArray(r.data) ? r.data : []))
      .catch((e) => setErr(apiError(e)));
  }, []);

  if (err) {
    return <div className="callout callout-red">{err}</div>;
  }
  if (!acct || !orders) {
    return <p className="muted">Loading account...</p>;
  }

  const credit = acct.credit || {};
  const limit = Number(credit.creditLimit || 0);
  const outstanding = Number(credit.outstanding || 0);
  const available = Number(credit.available || 0);
  const usedPct = limit > 0 ? Math.min(100, (outstanding / limit) * 100) : 0;
  const fill = usedPct >= 85 ? 'var(--accent)' : usedPct >= 60 ? 'var(--amber)' : 'var(--green)';

  const recent = orders.slice(0, 6);

  return (
    <div>
      <div className="stat-row">
        <div className="stat">
          <div className="stat-label">Credit available</div>
          <div className="stat-value">{fmtINR(available)}</div>
          <div className="stat-sub">of {fmtINR(limit)} limit</div>
        </div>
        <div className="stat">
          <div className="stat-label">Credit outstanding</div>
          <div className="stat-value">{fmtINR(outstanding)}</div>
          <div className="stat-sub">{usedPct.toFixed(0)}% of limit utilised</div>
        </div>
        <div className="stat">
          <div className="stat-label">Total orders</div>
          <div className="stat-value">{acct.orderCount}</div>
          <div className="stat-sub">all time</div>
        </div>
        <div className="stat">
          <div className="stat-label">Pending orders</div>
          <div className="stat-value">{acct.pendingCount}</div>
          <div className="stat-sub">awaiting approval</div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-h">
            <span className="card-title">Credit utilisation</span>
            <span className="mono muted small">{fmtINR(outstanding)} / {fmtINR(limit)}</span>
          </div>
          <div className="card-b">
            <div className="gauge">
              <div className="gauge-fill" style={{ width: `${usedPct}%`, background: fill }} />
            </div>
            <div className="gauge-legend">
              <span className="muted">Used {fmtINR(outstanding)}</span>
              <span style={{ color: fill, fontWeight: 600 }}>{fmtINR(available)} available</span>
            </div>
            <div className="note" style={{ marginTop: 12 }}>
              Outstanding covers orders in PENDING, APPROVED and SHIPPED state. Credit is released once an order is DELIVERED, REJECTED or CANCELLED.
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-h">
            <span className="card-title">{acct.companyName}</span>
          </div>
          <div className="card-b">
            <dl className="kv">
              <dt>GSTIN</dt>
              <dd className="mono">{acct.gstin || '-'}</dd>
              <dt>Phone</dt>
              <dd className="mono">{acct.phone || '-'}</dd>
              <dt>Registered user</dt>
              <dd>{user ? user.fullName : '-'}</dd>
            </dl>
            <div style={{ display: 'flex', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
              <Link to="/catalog" className="btn btn-primary">Browse catalog</Link>
              <Link to="/cart" className="btn">Open cart</Link>
              <Link to="/orders" className="btn">View orders</Link>
            </div>
          </div>
        </div>
      </div>

      <div className="card section-gap">
        <div className="card-h">
          <span className="card-title">Recent orders</span>
          <Link to="/orders" className="btn-link">View all</Link>
        </div>
        <div className="table-scroll">
          {recent.length === 0 ? (
            <div className="empty">
              <div className="empty-title">No orders yet</div>
              <div>Browse the catalog to place your first order.</div>
              <Link to="/catalog" className="btn btn-primary">Open catalog</Link>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Placed</th>
                  <th className="right">Items</th>
                  <th className="right">Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {recent.map((o) => (
                  <tr key={o.id} className="rowlink" onClick={() => nav(`/orders/${o.id}`)}>
                    <td className="mono">{o.orderNumber}</td>
                    <td className="nowrap">{fmtDate(o.createdAt)}</td>
                    <td className="right mono">{(o.items || []).length}</td>
                    <td className="right mono">{fmtINR(o.totalAmount)}</td>
                    <td><StatusChip status={o.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
