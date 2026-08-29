import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { apiError } from '../api';

const BLANK = { fullName: '', companyName: '', gstin: '', phone: '', email: '', password: '' };

export default function Register() {
  const { register } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState(BLANK);
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  async function submit(e) {
    e.preventDefault();
    setErr('');
    setBusy(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        companyName: form.companyName.trim(),
        gstin: form.gstin.trim().toUpperCase(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      nav('/');
    } catch (ex) {
      setErr(apiError(ex));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card" style={{ width: 440 }}>
        <div className="auth-logo">
          <span className="logo-mark" />
          ChannelHub
        </div>
        <p className="auth-sub">Create a reseller account</p>

        {err && (
          <div className="callout callout-red" style={{ marginBottom: 14 }}>
            {err}
          </div>
        )}

        <form onSubmit={submit}>
          <div className="form-grid">
            <div className="span2">
              <label className="lbl">Full name</label>
              <input className="input" value={form.fullName} onChange={set('fullName')} placeholder="e.g. Rohan Mehta" required />
            </div>
            <div className="span2">
              <label className="lbl">Company name</label>
              <input className="input" value={form.companyName} onChange={set('companyName')} placeholder="e.g. Mehta Computers & Peripherals" required />
            </div>
            <div>
              <label className="lbl">GSTIN</label>
              <input className="input input-mono" value={form.gstin} onChange={set('gstin')} placeholder="27ABCDE1234F1Z5" maxLength={15} required />
            </div>
            <div>
              <label className="lbl">Phone</label>
              <input className="input input-mono" value={form.phone} onChange={set('phone')} placeholder="98XXXXXXXX" maxLength={10} required />
            </div>
            <div className="span2">
              <label className="lbl">Email</label>
              <input className="input" type="email" value={form.email} onChange={set('email')} placeholder="you@company.in" required />
            </div>
            <div className="span2">
              <label className="lbl">Password</label>
              <input className="input" type="password" value={form.password} onChange={set('password')} placeholder="Minimum 8 characters" minLength={8} required />
            </div>
          </div>
          <button className="btn btn-primary" style={{ width: '100%', marginTop: 16 }} disabled={busy}>
            {busy ? 'Creating account...' : 'Create account'}
          </button>
        </form>

        <p className="note" style={{ marginTop: 10 }}>
          New accounts start with a credit limit of ₹2,00,000.00.
        </p>

        <div className="auth-foot">
          Already registered? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
