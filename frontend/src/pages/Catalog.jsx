import { useEffect, useState } from 'react';
import api, { apiError } from '../api';
import { fmtINR } from '../format';
import { addToCart } from '../cart';

const SIZE = 20;

function stockOf(p) {
  const v = p.stock ?? p.availableQty ?? p.qtyAvailable ?? p.totalStock ?? p.available;
  return v === undefined || v === null ? null : Number(v);
}

export default function Catalog() {
  const [meta, setMeta] = useState({ brands: [], categories: [] });
  const [qInput, setQInput] = useState('');
  const [q, setQ] = useState('');
  const [brand, setBrand] = useState('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [err, setErr] = useState('');
  const [qtys, setQtys] = useState({});
  const [addedId, setAddedId] = useState(null);

  useEffect(() => {
    api.get('/meta')
      .then((r) => setMeta(r.data || {}))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setQ(qInput.trim()), 300);
    return () => clearTimeout(t);
  }, [qInput]);

  useEffect(() => {
    setPage(0);
  }, [q, brand, category]);

  useEffect(() => {
    let live = true;
    const params = { page, size: SIZE };
    if (q) params.q = q;
    if (brand) params.brand = brand;
    if (category) params.category = category;
    api.get('/products', { params })
      .then((r) => {
        if (live) {
          setData(r.data);
          setErr('');
        }
      })
      .catch((e) => {
        if (live) setErr(apiError(e));
      });
    return () => {
      live = false;
    };
  }, [page, q, brand, category]);

  const rows = data ? data.content || [] : [];
  const total = data ? data.totalElements ?? rows.length : 0;
  const totalPages = data ? data.totalPages ?? 1 : 1;
  const from = total === 0 ? 0 : page * SIZE + 1;
  const to = Math.min(total, (page + 1) * SIZE);

  function add(p) {
    const qty = Math.max(1, parseInt(qtys[p.id], 10) || 1);
    addToCart({
      productId: p.id,
      sku: p.sku,
      name: p.name,
      brand: p.brand,
      unitPrice: Number(p.unitPrice),
      qty,
    });
    setAddedId(p.id);
    setTimeout(() => setAddedId((cur) => (cur === p.id ? null : cur)), 1200);
  }

  return (
    <div>
      <div className="toolbar">
        <div className="field field-q">
          <label className="lbl">Search SKU, name or brand</label>
          <input
            className="input"
            placeholder="e.g. C9200L or ProLiant"
            value={qInput}
            onChange={(e) => setQInput(e.target.value)}
          />
        </div>
        <div className="field">
          <label className="lbl">Brand</label>
          <select className="input" value={brand} onChange={(e) => setBrand(e.target.value)}>
            <option value="">All brands</option>
            {(meta.brands || []).map((b) => (
              <option key={b} value={b}>{b}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label className="lbl">Category</label>
          <select className="input" value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All categories</option>
            {(meta.categories || []).map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      </div>

      {err && <div className="callout callout-red" style={{ marginBottom: 12 }}>{err}</div>}

      <div className="card">
        <div className="table-scroll">
          {rows.length === 0 && !err ? (
            <div className="empty">
              <div className="empty-title">No products match</div>
              <div>Try a different search term, brand or category.</div>
            </div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>Brand</th>
                  <th>Category</th>
                  <th className="right">Unit price</th>
                  <th className="right">Stock</th>
                  <th className="right">Add</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((p) => {
                  const stock = stockOf(p);
                  return (
                    <tr key={p.id}>
                      <td className="mono">{p.sku}</td>
                      <td>
                        <div style={{ fontWeight: 500 }}>{p.name}</div>
                        {p.description && (
                          <div className="muted small nowrap" style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                            {p.description}
                          </div>
                        )}
                      </td>
                      <td><span className="chip">{p.brand}</span></td>
                      <td>{p.category}</td>
                      <td className="right mono">{fmtINR(p.unitPrice)}</td>
                      <td className="right">
                        {stock === null ? <span className="muted">-</span> : (
                          <span className="mono" style={{ color: stock === 0 ? 'var(--accent)' : undefined }}>
                            {stock}
                          </span>
                        )}
                      </td>
                      <td className="right nowrap">
                        <input
                          className="input input-mono qty-in"
                          type="number"
                          min="1"
                          value={qtys[p.id] || 1}
                          onChange={(e) => setQtys((s) => ({ ...s, [p.id]: e.target.value }))}
                          aria-label={`Quantity for ${p.sku}`}
                        />{' '}
                        <button className="btn btn-sm" onClick={() => add(p)}>
                          {addedId === p.id ? 'Added' : 'Add'}
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

      <div className="pager">
        <button className="btn btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
          Prev
        </button>
        <button className="btn btn-sm" disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
          Next
        </button>
        <span className="pager-info">
          Showing {from}-{to} of {total} products
        </span>
      </div>
    </div>
  );
}
