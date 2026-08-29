import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { apiError } from '../api';
import { fmtINR, fmtDate, StatusChip } from '../format';

export default function Orders() {
  const nav = useNavigate();
  const [rows, setRows] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    api.get('/orders')
      .then((r) => setRows(Array.isArray(r.data) ? r.data : []))
      .catch((e) => setErr(apiError(e)));
  }, []);

  if (err) return <div className="callout callout-red">{err}</div>;
  if (!rows) return <p className="muted">Loading orders...</p>;

  return (
    <div className="card">
      <div className="table-scroll">
        {rows.length === 0 ? (
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
                <th>Invoice #</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((o) => (
                <tr key={o.id} className="rowlink" onClick={() => nav(`/orders/${o.id}`)}>
                  <td className="mono">{o.orderNumber}</td>
                  <td className="nowrap">{fmtDate(o.createdAt)}</td>
                  <td className="right mono">{(o.items || []).length}</td>
                  <td className="right mono">{fmtINR(o.totalAmount)}</td>
                  <td className="mono">{o.invoiceNumber || <span className="muted">-</span>}</td>
                  <td><StatusChip status={o.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
