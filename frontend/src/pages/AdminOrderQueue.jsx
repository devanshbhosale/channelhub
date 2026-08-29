import { useEffect, useState } from 'react';
import api, { apiError } from '../api';
import { fmtINR, fmtDateTime, StatusChip } from '../format';

const TABS = ['PENDING', 'ALL', 'APPROVED', 'SHIPPED', 'DELIVERED', 'REJECTED', 'CANCELLED'];

export default function AdminOrderQueue() {
  const [tab, setTab] = useState('PENDING');
  const [rows, setRows] = useState(null);
  const [err, setErr] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [rejectFor, setRejectFor] = useState(null); // {id, orderNumber, reason}

  async function load(t) {
    try {
      const r = await api.get('/admin/orders', { params: t === 'ALL' ? {} : { status: t } });
      setRows(Array.isArray(r.data) ? r.data : []);
      setErr('');
    } catch (e) {
      setErr(apiError(e));
    }
  }

  useEffect(() => {
    load(tab);
  }, [tab]);

  async function act(o, status, reason) {
    setBusyId(o.id);
    setErr('');
    try {
      const body = reason ? { status, reason } : { status };
      await api.patch(`/admin/orders/${o.id}/status`, body);
      setRejectFor(null);
      await load(tab);
    } catch (e) {
      setErr(apiError(e));
    } finally {
      setBusyId(null);
    }
  }

  function confirmReject(o) {
    if (!rejectFor || !rejectFor.reason.trim()) {
      setErr('Rejection reason is required.');
      return;
    }
    act(o, 'REJECTED', rejectFor.reason.trim());
  }

  return (
    <div>
      <div className="tabs">
        {TABS.map((t) => (
          <button key={t} className={'tab' + (tab === t ? ' active' : '')} onClick={() => setTab(t)}>
            {t === 'ALL' ? 'All' : t.charAt(0) + t.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {err && <div className="callout callout-red" style={{ marginBottom: 12 }}>{err}</div>}

      <div className="card">
        <div className="table-scroll">
          {rows === null ? (
            <div className="empty">Loading orders...</div>
          ) : rows.length === 0 ? (
            <div className="empty">
              <div className="empty-title">No {tab === 'ALL' ? '' : tab.toLowerCase() + ' '}orders</div>
              <div>Orders will appear here as resellers place them.</div>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Reseller</th>
                  <th className="right">Total</th>
                  <th>Status</th>
                  <th>Placed</th>
                  <th className="right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((o) => (
                  <OrderRow
                    key={o.id ?? o.orderNumber}
                    o={o}
                    busy={busyId === o.id}
                    rejectFor={rejectFor}
                    setRejectFor={setRejectFor}
                    onAct={act}
                    onConfirmReject={confirmReject}
                  />
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function OrderRow({ o, busy, rejectFor, setRejectFor, onAct, onConfirmReject }) {
  const rejecting = rejectFor && rejectFor.id === o.id;
  return (
    <>
      <tr>
        <td className="mono">{o.orderNumber}</td>
        <td>{o.companyName}</td>
        <td className="right mono">{fmtINR(o.totalAmount)}</td>
        <td><StatusChip status={o.status} /></td>
        <td className="nowrap">{fmtDateTime(o.createdAt)}</td>
        <td className="right nowrap">
          {o.status === 'PENDING' && (
            <>
              <button className="btn btn-sm" disabled={busy} onClick={() => onAct(o, 'APPROVED')}>Approve</button>{' '}
              <button
                className="btn btn-sm"
                disabled={busy}
                onClick={() => setRejectFor(rejecting ? null : { id: o.id, reason: '' })}
              >
                Reject
              </button>
            </>
          )}
          {o.status === 'APPROVED' && (
            <button className="btn btn-sm" disabled={busy} onClick={() => onAct(o, 'SHIPPED')}>Mark shipped</button>
          )}
          {o.status === 'SHIPPED' && (
            <button className="btn btn-sm" disabled={busy} onClick={() => onAct(o, 'DELIVERED')}>Mark delivered</button>
          )}
          {['DELIVERED', 'REJECTED', 'CANCELLED'].includes(o.status) && <span className="muted small">No actions</span>}
        </td>
      </tr>
      {rejecting && (
        <tr>
          <td colSpan={6} style={{ background: '#fcf6ea' }}>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <span className="small" style={{ color: 'var(--amber)', fontWeight: 600, whiteSpace: 'nowrap' }}>
                Rejection reason for {o.orderNumber} (required)
              </span>
              <input
                className="input"
                style={{ maxWidth: 420 }}
                placeholder="e.g. Credit limit exceeded — clear dues and resubmit"
                value={rejectFor.reason}
                onChange={(e) => setRejectFor({ id: o.id, reason: e.target.value })}
                autoFocus
              />
              <button className="btn btn-sm" onClick={() => onConfirmReject(o)}>Confirm reject</button>
              <button className="btn-link" onClick={() => setRejectFor(null)}>Cancel</button>
            </div>
          </td>
        </tr>
      )}
    </>
  );
}
