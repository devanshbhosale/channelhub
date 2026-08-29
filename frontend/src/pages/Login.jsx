import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { apiError } from '../api';

const DEMO_RESELLER = { email: 'reseller.demo@channelhub.in', password: 'Demo@Reseller2026' };
const DEMO_ADMIN = { email: 'admin.demo@channelhub.in', password: 'Demo@Admin2026' };

export default function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function doLogin(em, pw) {
    setErr('');
    setBusy(true);
    try {
      const u = await login(em, pw);
      nav(u.role === 'ADMIN' ? '/admin' : '/');
    } catch (e) {
      setErr(apiError(e));
    } finally {
      setBusy(false);
    }
  }

  function submit(e) {
    e.preventDefault();
    doLogin(email.trim(), password);
  }

  function tryDemo(d) {
    setEmail(d.email);
    setPassword(d.password);
    doLogin(d.email, d.password);
  }

  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-logo">
          <span className="logo-mark" />
          ChannelHub
        </div>
        <p className="auth-sub">B2B ordering portal for IT channel partners</p>

        {err && (
          <div className="callout callout-red" style={{ marginBottom: 14 }}>
            {err}
          </div>
        )}

        <form onSubmit={submit}>
          <div className="auth-field">
            <label className="lbl" htmlFor="email">Email</label>
            <input
              id="email"
              className="input"
              type="email"
              autoComplete="username"
              placeholder="you@company.in"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="auth-field">
            <label className="lbl" htmlFor="password">Password</label>
            <input
              id="password"
              className="input"
              type="password"
              autoComplete="current-password"
              placeholder="Your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <button className="btn btn-primary" style={{ width: '100%' }} disabled={busy}>
            {busy ? 'Signing in...' : 'Sign in'}
          </button>
        </form>

        <div className="auth-divider">Try demo</div>
        <div className="auth-demo">
          <button className="btn" disabled={busy} onClick={() => tryDemo(DEMO_RESELLER)}>
            Reseller demo
          </button>
          <button className="btn" disabled={busy} onClick={() => tryDemo(DEMO_ADMIN)}>
            Admin demo
          </button>
        </div>
        <p className="auth-demo-note">
          Reseller: reseller.demo@channelhub.in / Admin (read-only): admin.demo@channelhub.in
        </p>

        <div className="auth-foot">
          New partner? <Link to="/register">Create a reseller account</Link>
        </div>
      </div>
    </div>
  );
}
