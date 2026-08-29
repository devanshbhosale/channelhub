import { useEffect, useState } from 'react';
import api, { apiError } from '../api';
import { fmtINR } from '../format';

function CreditBar({ outstanding, limit }) {
  const l = Number(limit || 0);
  const pct = l > 0 ? Math.min(100, (Number(outstanding || 0) / l) * 100) : 0;
  const color = pct >= 85 ? 'var(--accent)' : pct >= 60 ? 'var(--amber)' : 'var(--green)';
  return (
    <div>
      <div className="creditbar">
        <span style={{ width: `${pct}%`, background: color }} />
      </div>
      <div className="creditpct mono">{pct.toFixed(0)}% used</div>
    </div>
  );
}

export default function AdminResellers() {
  const [rows, setRows] = useState(null);
  const [err, setErr] = useState('');

  useEffect(() => {
    api.get('/admin/resellers')
      .then((r) => setRows(Array.isArray(r.data) ? r.data : []))
      .catch((e) => setErr(apiError(e)));
  }, []);

  if (err) return <div className="callout callout-red">{err}</div>;
  if (!rows) return <p className="muted">Loading resellers...</p>;

  return (
    <div className="card">
      <div className="table-scroll">
        {rows.length === 0 ? (
          <div className="empty">
            <div className="empty-title">No resellers registered</div>
            <div>Reseller accounts appear here once partners sign up.</div>
          </div>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Company</th>
                <th>GSTIN</th>
                <th>Phone</th>
                <th className="right">Orders</th>
                <th className="right">Credit limit</th>
                <th className="right">Outstanding</th>
                <th className="right">Available</th>
                <th>Credit utilisation</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={i}>
                  <td style={{ fontWeight: 500 }}>{r.companyName}</td>
                  <td className="mono">{r.gstin || '-'}</td>
                  <td className="mono">{r.phone || '-'}</td>
                  <td className="right mono">{r.orderCount ?? 0}</td>
                  <td className="right mono">{fmtINR(r.creditLimit)}</td>
                  <td className="right mono">{fmtINR(r.outstanding)}</td>
                  <td className="right mono" style={{ fontWeight: 600 }}>{fmtINR(r.available)}</td>
                  <td><CreditBar outstanding={r.outstanding} limit={r.creditLimit} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
