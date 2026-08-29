const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export function fmtINR(value) {
  const n = Number(value);
  return Number.isFinite(n) ? inr.format(n) : '-';
}

function parts(value) {
  if (!value) return null;
  const d = new Date(value);
  if (isNaN(d.getTime())) return null;
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const hh = String(d.getHours()).padStart(2, '0');
  const mi = String(d.getMinutes()).padStart(2, '0');
  return { dd, mmm: MONTHS[d.getMonth()], yyyy: d.getFullYear(), mm, hh, mi };
}

// DD-MMM-YYYY, e.g. 29-Aug-2026
export function fmtDate(value) {
  const p = parts(value);
  return p ? `${p.dd}-${p.mmm}-${p.yyyy}` : '-';
}

// DD-MMM-YYYY HH:mm
export function fmtDateTime(value) {
  const p = parts(value);
  return p ? `${p.dd}-${p.mmm}-${p.yyyy} ${p.hh}:${p.mi}` : '-';
}

// 16-Aug, for chart axes
export function fmtDayShort(value) {
  const p = parts(value ? `${value}T00:00:00` : value);
  return p ? `${p.dd} ${p.mmm}` : '-';
}

// Maps an OrderStatus to its chip tone class.
export function statusTone(status) {
  switch (status) {
    case 'PENDING':
      return 'amber';
    case 'APPROVED':
    case 'SHIPPED':
    case 'DELIVERED':
      return 'green';
    case 'REJECTED':
      return 'red';
    case 'CANCELLED':
      return 'grey';
    default:
      return 'grey';
  }
}

export function StatusChip({ status }) {
  return <span className={`chip chip-${statusTone(status)}`}>{status}</span>;
}

export { MONTHS };
