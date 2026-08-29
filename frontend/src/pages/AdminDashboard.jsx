import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import Chart from 'chart.js/auto';
import api, { apiError } from '../api';
import { fmtINR, fmtDayShort, fmtDateTime, StatusChip } from '../format';

const FONT_SANS = "'IBM Plex Sans', sans-serif";
const FONT_MONO = "'IBM Plex Mono', monospace";

export default function AdminDashboard() {
  const [d, setD] = useState(null);
  const [err, setErr] = useState('');
  const canvasRef = useRef(null);

  useEffect(() => {
    api.get('/admin/dashboard')
      .then((r) => setD(r.data))
      .catch((e) => setErr(apiError(e)));
  }, []);

  useEffect(() => {
    if (!d || !canvasRef.current || !Array.isArray(d.series14d)) return;
    const compact = new Intl.NumberFormat('en-IN', { notation: 'compact' });
    const chart = new Chart(canvasRef.current, {
      type: 'line',
      data: {
        labels: d.series14d.map((s) => fmtDayShort(s.date)),
        datasets: [
          {
            label: 'Orders',
            data: d.series14d.map((s) => Number(s.orders) || 0),
            borderColor: '#b3261e',
            backgroundColor: '#b3261e',
            borderWidth: 2,
            pointRadius: 2,
            tension: 0.25,
            yAxisID: 'y',
          },
          {
            label: 'Revenue',
            data: d.series14d.map((s) => Number(s.revenue) || 0),
            borderColor: '#15803d',
            backgroundColor: '#15803d',
            borderWidth: 2,
            pointRadius: 2,
            tension: 0.25,
            yAxisID: 'y1',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: {
            labels: { boxWidth: 10, boxHeight: 2, font: { family: FONT_SANS, size: 11.5 }, color: '#6a6f76' },
          },
          tooltip: {
            titleFont: { family: FONT_SANS, size: 11.5 },
            bodyFont: { family: FONT_MONO, size: 11.5 },
            callbacks: {
              label: (ctx) => (ctx.datasetIndex === 1 ? ` Revenue: ₹${compact.format(ctx.parsed.y)}` : ` Orders: ${ctx.parsed.y}`),
            },
          },
        },
        scales: {
          x: {
            grid: { color: '#efeeea' },
            ticks: { font: { family: FONT_MONO, size: 10.5 }, color: '#6a6f76' },
          },
          y: {
            beginAtZero: true,
            title: { display: true, text: 'Orders', font: { family: FONT_SANS, size: 10.5 }, color: '#6a6f76' },
            ticks: { precision: 0, font: { family: FONT_MONO, size: 10.5 }, color: '#6a6f76' },
            grid: { color: '#efeeea' },
          },
          y1: {
            position: 'right',
            beginAtZero: true,
            title: { display: true, text: 'Revenue', font: { family: FONT_SANS, size: 10.5 }, color: '#6a6f76' },
            ticks: { font: { family: FONT_MONO, size: 10.5 }, color: '#6a6f76', callback: (v) => '₹' + compact.format(v) },
            grid: { drawOnChartArea: false },
          },
        },
      },
    });
    return () => chart.destroy();
  }, [d]);

  if (err) return <div className="callout callout-red">{err}</div>;
  if (!d) return <p className="muted">Loading dashboard...</p>;

  const sc = d.statCards || {};
  const lowStock = d.lowStock || [];
  const topSkus = d.topSkus || [];
  const queue = d.queue || [];

  return (
    <div>
      <div className="stat-row">
        <div className="stat">
          <div className="stat-label">Pending orders</div>
          <div className="stat-value">{sc.pending ?? 0}</div>
          <div className="stat-sub">awaiting approval</div>
        </div>
        <div className="stat">
          <div className="stat-label">Approved orders</div>
          <div className="stat-value">{sc.approved ?? 0}</div>
          <div className="stat-sub">ready to ship</div>
        </div>
        <div className="stat">
          <div className="stat-label">Low stock rows</div>
          <div className="stat-value" style={{ color: (sc.lowStock || 0) > 0 ? 'var(--amber)' : undefined }}>{sc.lowStock ?? 0}</div>
          <div className="stat-sub">at or below reorder level</div>
        </div>
        <div className="stat">
          <div className="stat-label">Revenue this month</div>
          <div className="stat-value">{fmtINR(sc.revenueThisMonth || 0)}</div>
          <div className="stat-sub">delivered + invoiced</div>
        </div>
      </div>

      <div className="card">
        <div className="card-h">
          <span className="card-title">Last 14 days — orders and revenue</span>
        </div>
        <div className="card-b">
          <div className="chart-box">
            <canvas ref={canvasRef} />
          </div>
        </div>
      </div>

      <div className="grid-2 section-gap">
        <div className="card">
          <div className="card-h"><span className="card-title">Top SKUs by quantity sold</span></div>
          <div className="table-scroll">
            {topSkus.length === 0 ? (
              <div className="empty">No sales recorded in this period yet.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Product</th>
                    <th className="right">Qty sold</th>
                  </tr>
                </thead>
                <tbody>
                  {topSkus.map((s, i) => (
                    <tr key={i}>
                      <td className="mono">{s.sku}</td>
                      <td>{s.name}</td>
                      <td className="right mono">{s.qtySold}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-h">
            <span className="card-title">Low stock</span>
            <Link to="/admin/inventory" className="btn-link">Manage inventory</Link>
          </div>
          <div className="table-scroll">
            {lowStock.length === 0 ? (
              <div className="empty">All stock levels are healthy.</div>
            ) : (
              <table className="table">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Warehouse</th>
                    <th className="right">Qty</th>
                    <th className="right">Reorder</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStock.map((r, i) => (
                    <tr key={i}>
                      <td className="mono">{r.sku}</td>
                      <td><span className="chip chip-wh">{r.warehouse}</span></td>
                      <td className="right mono" style={{ color: 'var(--amber)', fontWeight: 600 }}>{r.quantity}</td>
                      <td className="right mono">{r.reorderLevel}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>

      <div className="card section-gap">
        <div className="card-h">
          <span className="card-title">Order queue — latest</span>
          <Link to="/admin/queue" className="btn-link">Open order queue</Link>
        </div>
        <div className="table-scroll">
          {queue.length === 0 ? (
            <div className="empty">No orders in the system yet.</div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Order #</th>
                  <th>Reseller</th>
                  <th className="right">Total</th>
                  <th>Status</th>
                  <th>Placed</th>
                </tr>
              </thead>
              <tbody>
                {queue.map((o, i) => (
                  <tr key={i}>
                    <td className="mono">{o.orderNumber}</td>
                    <td>{o.companyName}</td>
                    <td className="right mono">{fmtINR(o.totalAmount)}</td>
                    <td><StatusChip status={o.status} /></td>
                    <td className="nowrap">{fmtDateTime(o.createdAt)}</td>
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
