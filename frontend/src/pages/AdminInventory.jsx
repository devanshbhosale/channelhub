import { useEffect, useState } from 'react';
import api, { apiError } from '../api';

export default function AdminInventory() {
  const [rows, setRows] = useState(null);
  const [lowOnly, setLowOnly] = useState(false);
  const [deltas, setDeltas] = useState({});
  const [msg, setMsg] = useState(null); // {tone, text}
  const [err, setErr] = useState('');
  const [busyKey, setBusyKey] = useState(null);

  async function load(low) {
    try {
      const r = await api.get('/admin/inventory', { params: low ? { lowStock: true } : {} });
      setRows(Array.isArray(r.data) ? r.data : []);
      setErr('');
    } catch (e) {
      setErr(apiError(e));
    }
  }

  useEffect(() => {
    load(lowOnly);
  }, [lowOnly]);

  function flash(tone, text) {
    setMsg({ tone, text });
    setTimeout(() => setMsg(null), 3500);
  }

  async function adjust(r) {
    const key = `${r.productId}:${r.warehouse}`;
    const delta = parseInt(deltas[key], 10);
    if (Number.isNaN(delta) || delta === 0) {
      flash('amber', 'Enter a non-zero adjustment (use a negative number to reduce stock).');
      return;
    }
    setBusyKey(key);
    try {
      await api.post('/admin/inventory/adjust', { productId: r.productId, warehouse: r.warehouse, delta });
      flash('green', `Stock updated for ${r.sku} at ${r.warehouse} (${delta > 0 ? '+' : ''}${delta}).`);
      setDeltas((s) => ({ ...s, [key]: '' }));
      await load(lowOnly);
    } catch (e) {
      flash('red', apiError(e));
    } finally {
      setBusyKey(null);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <label className="check">
          <input type="checkbox" checked={lowOnly} onChange={(e) => setLowOnly(e.target.checked)} />
          Low stock only
        </label>
        <span className="spacer" />
        <span className="note">Warehouses: BOM-01 Mumbai · MAA-01 Chennai · DEL-01 Delhi</span>
      </div>

      {msg && <div className={`callout callout-${msg.tone === 'red' ? 'red' : msg.tone === 'amber' ? 'amber' : 'green'}`} style={{ marginBottom: 12 }}>{msg.text}</div>}
      {err && <div className="callout callout-red" style={{ marginBottom: 12 }}>{err}</div>}

      <div className="card">
        <div className="table-scroll">
          {rows === null ? (
            <div className="empty">Loading inventory...</div>
          ) : rows.length === 0 ? (
            <div className="empty">
              <div className="empty-title">No inventory rows</div>
              <div>{lowOnly ? 'All stock levels are healthy.' : 'Inventory rows appear here once products are created.'}</div>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>Warehouse</th>
                  <th className="right">Quantity</th>
                  <th className="right">Reorder level</th>
                  <th>Status</th>
                  <th className="right">Adjust stock</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => {
                  const low = r.quantity <= r.reorderLevel;
                  const key = `${r.productId}:${r.warehouse}`;
                  return (
                    <tr key={i} className={low ? 'row-low' : ''}>
                      <td className="mono">{r.sku}</td>
                      <td>{r.name}</td>
                      <td><span className="chip chip-wh">{r.warehouse}</span></td>
                      <td className="right mono" style={{ fontWeight: 600 }}>{r.quantity}</td>
                      <td className="right mono">{r.reorderLevel}</td>
                      <td>
                        {low ? <span className="chip chip-amber">Low stock</span> : <span className="chip">OK</span>}
                      </td>
                      <td className="right nowrap">
                        <input
                          className="input input-mono delta-in"
                          type="number"
                          placeholder="+/-"
                          value={deltas[key] || ''}
                          onChange={(e) => setDeltas((s) => ({ ...s, [key]: e.target.value }))}
                          aria-label={`Stock delta for ${r.sku} at ${r.warehouse}`}
                        />{' '}
                        <button className="btn btn-sm" disabled={busyKey === key} onClick={() => adjust(r)}>
                          Apply
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
