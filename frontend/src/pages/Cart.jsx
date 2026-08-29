import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';
import { fmtINR } from '../format';
import { useCart, updateQty, removeFromCart, clearCart, cartTotal } from '../cart';

export default function Cart() {
  const nav = useNavigate();
  const items = useCart();
  const [acct, setAcct] = useState(null);
  const [notes, setNotes] = useState('');
  const [err, setErr] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api.get('/account')
      .then((r) => setAcct(r.data))
      .catch(() => {});
  }, []);

  const total = cartTotal(items);
  const credit = acct && acct.credit;
  const available = credit ? Number(credit.available) : null;
  const remaining = available === null ? null : available - total;

  async function place() {
    setBusy(true);
    setErr(null);
    const payload = { items: items.map((i) => ({ productId: i.productId, quantity: i.qty })) };
    const n = notes.trim();
    if (n) payload.notes = n;
    try {
      const r = await api.post('/orders', payload);
      clearCart();
      nav(`/orders/${r.data.id}`);
    } catch (e) {
      const d = e.response && e.response.data;
      setErr(d && typeof d === 'object' && !Array.isArray(d) ? d : { error: String(d && d.error ? d.error : e.message) });
    } finally {
      setBusy(false);
    }
  }

  if (items.length === 0) {
    return (
      <div className="card">
        <div className="empty">
          <div className="empty-title">Your cart is empty</div>
          <div>Add items from the catalog to build an order.</div>
          <Link to="/catalog" className="btn btn-primary">Open catalog</Link>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="card">
        <div className="card-h">
          <span className="card-title">Order lines</span>
          <button className="btn-link" onClick={clearCart}>Clear cart</button>
        </div>
        <div className="table-scroll">
          <table className="table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product</th>
                <th className="right">Unit price</th>
                <th className="right">Qty</th>
                <th className="right">Amount</th>
                <th className="right">Remove</th>
              </tr>
            </thead>
            <tbody>
              {items.map((i) => (
                <tr key={i.productId}>
                  <td className="mono">{i.sku}</td>
                  <td>
                    <div style={{ fontWeight: 500 }}>{i.name}</div>
                    {i.brand && <div className="muted small">{i.brand}</div>}
                  </td>
                  <td className="right mono">{fmtINR(i.unitPrice)}</td>
                  <td className="right">
                    <input
                      className="input input-mono qty-in"
                      type="number"
                      min="1"
                      value={i.qty}
                      onChange={(e) => {
                        const v = parseInt(e.target.value, 10);
                        if (!Number.isNaN(v)) updateQty(i.productId, v);
                      }}
                      aria-label={`Quantity for ${i.sku}`}
                    />
                  </td>
                  <td className="right mono">{fmtINR(Number(i.unitPrice) * i.qty)}</td>
                  <td className="right">
                    <button className="btn-link" onClick={() => removeFromCart(i.productId)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
            <tfoot>
              <tr>
                <td colSpan={4} className="right">Order total</td>
                <td className="right mono">{fmtINR(total)}</td>
                <td />
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <div className="grid-2 section-gap">
        <div className="card">
          <div className="card-h"><span className="card-title">Notes for the distributor</span></div>
          <div className="card-b">
            <textarea
              className="input"
              rows={3}
              placeholder="Optional — PO reference, delivery instructions, preferred warehouse..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
        </div>

        <div className="card">
          <div className="card-h"><span className="card-title">Credit preview</span></div>
          <div className="card-b">
            {credit ? (
              remaining >= 0 ? (
                <p className="note">
                  After placing this order, <span className="mono" style={{ color: 'var(--green)', fontWeight: 600 }}>{fmtINR(remaining)}</span> of{' '}
                  <span className="mono">{fmtINR(credit.creditLimit)}</span> credit will remain available (currently {fmtINR(available)}).
                </p>
              ) : (
                <p className="note" style={{ color: 'var(--accent)' }}>
                  Order total exceeds your available credit of <span className="mono">{fmtINR(available)}</span>. The order will be rejected at placement.
                </p>
              )
            ) : (
              <p className="note">Loading credit summary...</p>
            )}

            {err && (
              <div className="callout callout-red" style={{ marginTop: 12 }}>
                <div className="callout-title">{err.error || 'Order could not be placed'}</div>
                {Array.isArray(err.details) && err.details.length > 0 && (
                  <ul className="err-list">
                    {err.details.map((d, i) => (
                      <li key={i}>
                        <span className="mono">{d.sku}</span> — requested <span className="mono">{d.requested}</span>, available{' '}
                        <span className="mono">{d.available}</span>
                      </li>
                    ))}
                  </ul>
                )}
                {err.creditLimit != null && (
                  <div className="mono err-nums">
                    Limit {fmtINR(err.creditLimit)} · Outstanding {fmtINR(err.outstanding)} · Order total {fmtINR(err.orderTotal)}
                  </div>
                )}
              </div>
            )}

            <button className="btn btn-primary" style={{ width: '100%', marginTop: 14 }} disabled={busy} onClick={place}>
              {busy ? 'Placing order...' : `Place order · ${fmtINR(total)}`}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
