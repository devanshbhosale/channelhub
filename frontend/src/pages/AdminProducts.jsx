import { useEffect, useState } from 'react';
import api, { apiError } from '../api';
import { fmtINR } from '../format';

const BLANK = { sku: '', name: '', brand: '', category: '', description: '', unitPrice: '', active: true };

// Prefers the admin listing (includes inactive products); falls back to the
// shared catalog endpoint if the backend does not expose /api/admin/products.
async function fetchProducts() {
  try {
    const r = await api.get('/admin/products', { params: { page: 0, size: 500 } });
    return r.data;
  } catch (e) {
    const st = e.response && e.response.status;
    if (st === 403 || st === 404 || st === 405) {
      const r = await api.get('/products', { params: { page: 0, size: 500 } });
      return r.data;
    }
    throw e;
  }
}

export default function AdminProducts() {
  const [rows, setRows] = useState(null);
  const [err, setErr] = useState('');
  const [editing, setEditing] = useState(null); // 'new' | product object
  const [form, setForm] = useState(BLANK);
  const [formErr, setFormErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function load() {
    try {
      const d = await fetchProducts();
      const list = Array.isArray(d) ? d : d.content || [];
      setRows(list);
      setErr('');
    } catch (e) {
      setErr(apiError(e));
    }
  }

  useEffect(() => {
    load();
  }, []);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  function openNew() {
    setForm(BLANK);
    setFormErr('');
    setEditing('new');
  }

  function openEdit(p) {
    setForm({
      sku: p.sku || '',
      name: p.name || '',
      brand: p.brand || '',
      category: p.category || '',
      description: p.description || '',
      unitPrice: p.unitPrice ?? '',
      active: p.active !== false,
    });
    setFormErr('');
    setEditing(p);
  }

  async function save() {
    if (!form.sku.trim() || !form.name.trim() || !form.brand.trim() || !form.category.trim() || form.unitPrice === '') {
      setFormErr('SKU, name, brand, category and unit price are required.');
      return;
    }
    setBusy(true);
    setFormErr('');
    const payload = {
      sku: form.sku.trim(),
      name: form.name.trim(),
      brand: form.brand.trim(),
      category: form.category.trim(),
      description: form.description.trim(),
      unitPrice: Number(form.unitPrice),
      active: !!form.active,
    };
    try {
      if (editing === 'new') await api.post('/admin/products', payload);
      else await api.put(`/admin/products/${editing.id}`, payload);
      setEditing(null);
      await load();
    } catch (e) {
      setFormErr(apiError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="toolbar">
        <span className="note">
          {rows ? `${rows.length} products listed. Deactivated products are hidden from the reseller catalog.` : ''}
        </span>
        <span className="spacer" />
        <button className="btn btn-primary" onClick={openNew}>New product</button>
      </div>

      {err && <div className="callout callout-red" style={{ marginBottom: 12 }}>{err}</div>}

      <div className="card">
        <div className="table-scroll">
          {rows === null ? (
            <div className="empty">Loading products...</div>
          ) : rows.length === 0 ? (
            <div className="empty">
              <div className="empty-title">No products yet</div>
              <div>Create the first catalog product to get started.</div>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Name</th>
                  <th>Brand</th>
                  <th>Category</th>
                  <th className="right">Unit price</th>
                  <th>Status</th>
                  <th className="right">Edit</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((p) => (
                  <tr key={p.id}>
                    <td className="mono">{p.sku}</td>
                    <td>{p.name}</td>
                    <td><span className="chip">{p.brand}</span></td>
                    <td>{p.category}</td>
                    <td className="right mono">{fmtINR(p.unitPrice)}</td>
                    <td>
                      {p.active !== false ? <span className="chip chip-green">Active</span> : <span className="chip">Inactive</span>}
                    </td>
                    <td className="right">
                      <button className="btn btn-sm" onClick={() => openEdit(p)}>Edit</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {editing && (
        <div className="modal-overlay" onMouseDown={(e) => { if (e.target === e.currentTarget) setEditing(null); }}>
          <div className="modal">
            <div className="card-h">
              <span className="card-title">{editing === 'new' ? 'New product' : `Edit product — ${form.sku}`}</span>
              <button className="btn-link" onClick={() => setEditing(null)}>Close</button>
            </div>
            <div className="card-b">
              {formErr && <div className="callout callout-red" style={{ marginBottom: 12 }}>{formErr}</div>}
              <div className="form-grid">
                <div>
                  <label className="lbl">SKU</label>
                  <input className="input input-mono" value={form.sku} onChange={set('sku')} placeholder="e.g. C9200L-24P-4G" />
                </div>
                <div>
                  <label className="lbl">Brand</label>
                  <input className="input" value={form.brand} onChange={set('brand')} placeholder="e.g. Cisco" />
                </div>
                <div className="span2">
                  <label className="lbl">Name</label>
                  <input className="input" value={form.name} onChange={set('name')} />
                </div>
                <div>
                  <label className="lbl">Category</label>
                  <input className="input" value={form.category} onChange={set('category')} placeholder="e.g. Switching" />
                </div>
                <div>
                  <label className="lbl">Unit price (INR)</label>
                  <input className="input input-mono" type="number" min="0" step="0.01" value={form.unitPrice} onChange={set('unitPrice')} />
                </div>
                <div className="span2">
                  <label className="lbl">Description</label>
                  <textarea className="input" rows={2} value={form.description} onChange={set('description')} />
                </div>
                <label className="check">
                  <input type="checkbox" checked={form.active} onChange={(e) => setForm((f) => ({ ...f, active: e.target.checked }))} />
                  Active (visible in reseller catalog)
                </label>
              </div>
              <div className="modal-actions">
                <button className="btn" onClick={() => setEditing(null)}>Cancel</button>
                <button className="btn btn-primary" disabled={busy} onClick={save}>
                  {busy ? 'Saving...' : 'Save product'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
