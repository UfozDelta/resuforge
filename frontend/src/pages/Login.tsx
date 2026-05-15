import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';

export function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [u, setU] = useState('');
  const [p, setP] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setErr(null);
    setBusy(true);
    try {
      await login(u, p);
      nav('/projects', { replace: true });
    } catch {
      setErr('Invalid credentials.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="center-page">
      <div style={{ width: 460, maxWidth: '100%' }}>
        <div style={{ marginBottom: 28, borderBottom: '3px solid var(--ink)', paddingBottom: 14 }}>
          <div className="label muted">VOL.0 / ENTRY</div>
          <h1 className="display" style={{ fontSize: 56, margin: '6px 0 0' }}>
            Resume<span style={{ fontFamily: 'var(--mono)', fontStyle: 'normal', fontWeight: 700, fontSize: '0.5em' }}> // </span>Pipeline
          </h1>
          <div className="editorial muted" style={{ fontSize: 16, marginTop: 8 }}>
            A single-user atelier for tailoring résumés.
          </div>
        </div>

        <form onSubmit={submit} className="stack">
          <label className="field">
            <div className="field__label">Username</div>
            <input
              className="field__input"
              autoFocus
              autoComplete="username"
              value={u}
              onChange={e => setU(e.target.value)}
            />
          </label>
          <label className="field">
            <div className="field__label">Password</div>
            <input
              className="field__input"
              type="password"
              autoComplete="current-password"
              value={p}
              onChange={e => setP(e.target.value)}
            />
          </label>

          {err && <div className="err">{err}</div>}

          <div className="row row--between row--centered" style={{ marginTop: 12 }}>
            <span className="label muted">SESSION COOKIE · LOCALHOST</span>
            <button className="btn btn--acid" type="submit" disabled={busy}>
              {busy ? <span className="spinner">SIGNING IN</span> : <>ENTER &nbsp;→</>}
            </button>
          </div>
        </form>

        <div style={{ marginTop: 56, fontSize: 11, color: 'var(--muted)', letterSpacing: '0.15em', textTransform: 'uppercase' }}>
          ────────  one user · one resume · one job at a time
        </div>
      </div>
    </div>
  );
}
