import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { apiError } from '../api';
import { fmtINR, fmtDateTime, StatusChip } from '../format';

const FLOW = ['PENDING', 'APPROVED', 'SHIPPED', 'DELIVERED'];

function Timeline({ status }) {
  if (status === 'REJECTED' || status === 'CANCELLED') {
    const tone = status === 'REJECTED' ? 'red' : 'grey';
    return (
      <div className="timeline">
        {FLOW.map((s, i) => (
          <div className="tl-step" key={s}>
            {i > 0 && <span className="tl-bar" />}
            <span className="tl-dot" />
            <span className="tl-label">{s}</span>
          </div>
        ))}
        <div className="tl-step">
          <span className={`tl-bar tl-bar-${tone}`} />
          <span className={`tl-dot tl-dot-${tone}`} />
          <span className={`tl-label tl-label-${tone}`}>{status}</span>
        </div>
      </div>
    );
  }
  const idx = FLOW.indexOf(status);
  return (
    <div className="timeline">
      {FLOW.map((s, i) => {
        const done = i < idx || (i === idx && status === 'DELIVERED');
        const current = i === idx && status !== 'DELIVERED';
        return (
          <div className="tl-step" key={s}>
            {i > 0 && <span className={'tl-bar' + (i <= idx ? ' tl-bar-done' : '')} />}
            <span className={'tl-dot' + (done ? ' tl-dot-done' : current ? ' tl-dot-current' : '')} />
            <span className={'tl-label' + (i <= idx ? ' tl-label-active' : '')}>{s}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function OrderDetail() {
  const { id } = useParams();
  const [order, setOrder] = useState(null);
  const [err, setErr] = useState('');
  const [cancelErr, setCancelErr] = useState('');
  const [invErr, setInvErr] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get(`/orders/${id}`)
      .then((r) => setOrder(r.data))
      .catch((e) => setErr(apiError(e)));
  }, [id]);

  async function cancel() {
    setBusy(true);
    setCancelErr('');
    try {
      const r = await api.post(`/orders/${id}/cancel`);
      setOrder(r.data);
    } catch (e) {
      setCancelErr(apiError(e));
    } finally {
      setBusy(false);
    }
  }

  async function downloadInvoice() {
    setInvErr('');
    try {
      const res = await api.get(`/orders/${id}/invoice`, { responseType: 'blob' });
      const url = URL.createObjectURL(res.data);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Invoice-${order.invoiceNumber || order.orderNumber}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setInvErr(apiError(e));
    }
  }

  if (err) {
    return (
      <div>
        <div className="callout callout-red">{err}</div>
        <p style={{ marginTop: 12 }}>
          <Link to="/orders" className="btn-link">Back to orders</Link>
        </p>
      </div>
    );
  }
  if (!order) return <p className="muted">Loading order...</p>;

  const items = order.items || [];
  const canInvoice = order.invoiceNumber || ['APPROVED', 'SHIPPED', 'DELIVERED'].includes(order.status);

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: 14 }}>
        <div>
          <div className="mono" style={{ fontSize: 16, fontWeight: 600 }}>{order.orderNumber}</div>
          <div className="muted small">Placed {fmtDateTime(order.createdAt)}</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          {order.status === 'PENDING' && (
            <button className="btn" disabled={busy} onClick={cancel}>Cancel order</button>
          )}
          {canInvoice && (
            <button className="btn btn-primary" onClick={downloadInvoice}>Download GST Invoice</button>
          )}
        </div>
      </div>

      {cancelErr && <div className="callout callout-red" style={{ marginBottom: 14 }}>{cancelErr}</div>}
      {invErr && <div className="callout callout-red" style={{ marginBottom: 14 }}>Invoice download failed: {invErr}</div>}
      {order.status === 'REJECTED' && order.rejectionReason && (
        <div className="callout callout-red" style={{ marginBottom: 14 }}>
          <div className="callout-title">Order rejected</div>
          <div>{order.rejectionReason}</div>
        </div>
      )}
      {order.status === 'CANCELLED' && (
        <div className="callout callout-amber" style={{ marginBottom: 14 }}>
          This order was cancelled before approval.
        </div>
      )}

      <div className="card">
        <div className="card-h">
          <span className="card-title">Status</span>
          <StatusChip status={order.status} />
        </div>
        <div className="card-b">
          <Timeline status={order.status} />
        </div>
      </div>

      <div className="card section-gap">
        <div className="card-h"><span className="card-title">Order details</span></div>
        <div className="card-b">
          <div className="info-grid">
            <dl className="kv">
              <dt>Order number</dt>
              <dd className="mono">{order.orderNumber}</dd>
              <dt>Total amount</dt>
              <dd className="mono" style={{ fontWeight: 600 }}>{fmtINR(order.totalAmount)}</dd>
              <dt>Invoice number</dt>
              <dd className="mono">{order.invoiceNumber || '-'}</dd>
            </dl>
            <dl className="kv">
              <dt>Placed at</dt>
              <dd>{fmtDateTime(order.createdAt)}</dd>
              <dt>Notes</dt>
              <dd>{order.notes || <span className="muted">None</span>}</dd>
            </dl>
          </div>
        </div>
      </div>

      <div className="card section-gap">
        <div className="card-h"><span className="card-title">Items</span></div>
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product</th>
                <th className="right">Qty</th>
                <th className="right">Rate</th>
                <th className="right">Amount</th>
              </tr>
            </thead>
            <tbody>
              {items.map((it, i) => (
                <tr key={i}>
                  <td className="mono">{it.sku}</td>
                  <td>{it.name}</td>
                  <td className="right mono">{it.quantity}</td>
                  <td className="right mono">{fmtINR(it.unitPrice)}</td>
                  <td className="right mono">{fmtINR(Number(it.unitPrice) * it.quantity)}</td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4} className="right">Total (tax-exclusive, GST billed on invoice)</td>
                <td className="right mono">{fmtINR(order.totalAmount)}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  );
}
