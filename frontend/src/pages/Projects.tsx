import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { api, type Project, type ProjectKind } from '../lib/api';
import { Section } from '../components/Section';

interface Props { kind: ProjectKind }

export function Projects({ kind }: Props) {
  const isExperience = kind === 'EXPERIENCE';
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [sourcePath, setSourcePath] = useState('');
  const [title, setTitle] = useState('');
  const [company, setCompany] = useState('');
  const [location, setLocation] = useState('');
  const [dates, setDates] = useState('');

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  async function load() {
    setLoading(true);
    try { setProjects(await api.get<Project[]>(`/api/projects?kind=${kind}`)); }
    finally { setLoading(false); }
  }
  useEffect(() => { load(); }, [kind]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setErr(null); setBusy(true);
    try {
      const body: any = {
        kind, name, description,
        sourcePath: sourcePath || null,
      };
      if (isExperience) {
        body.title = title; body.company = company; body.location = location; body.dates = dates;
      }
      await api.post<Project>('/api/projects', body);
      setName(''); setDescription(''); setSourcePath('');
      setTitle(''); setCompany(''); setLocation(''); setDates('');
      setShowForm(false);
      await load();
    } catch (e: any) {
      setErr(e?.message || 'Failed to create');
    } finally {
      setBusy(false);
    }
  }

  const sectionNum = isExperience ? '02' : '01';
  const sectionTitle = isExperience ? 'Experiences' : 'Projects';
  const newButtonLabel = isExperience ? '+ NEW EXPERIENCE' : '+ NEW PROJECT';
  const newFormLabel = isExperience ? 'NEW EXPERIENCE / ROLE' : 'NEW PROJECT';

  return (
    <div className="shell">
      <Section num={sectionNum} title={sectionTitle} count={projects.length} />

      {loading ? <span className="spinner">LOADING</span> : (
        <>
          <div className="list">
            {projects.map((p, i) => (
              <Link key={p.id} to={`/projects/${p.id}`} className="list__row">
                <div className="list__num">{String(i + 1).padStart(2, '0')}</div>
                <div>
                  <h3 className="list__title">
                    {isExperience ? (p.title || p.name) : p.name}
                  </h3>
                  <div className="list__meta">
                    {isExperience
                      ? `${p.company || '—'} · ${p.location || '—'} · ${p.dates || '—'}`
                      : (p.sourcePath ? p.sourcePath : 'description-only')
                    }
                  </div>
                </div>
                <span className="list__arrow">→</span>
              </Link>
            ))}
            {projects.length === 0 && (
              <div style={{ padding: '40px 0', borderBottom: 'var(--rule-thin)' }} className="editorial muted">
                Nothing yet. {isExperience ? 'Add a role to start.' : 'The atelier opens with a single line.'}
              </div>
            )}
          </div>

          <div style={{ marginTop: 28 }}>
            {!showForm ? (
              <button className="btn btn--acid" onClick={() => setShowForm(true)}>{newButtonLabel}</button>
            ) : (
              <form onSubmit={submit} className="panel panel--inset stack" style={{ marginTop: 12 }}>
                <div className="label">{newFormLabel}</div>

                {isExperience && (
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                    <label className="field">
                      <div className="field__label">Job Title</div>
                      <input className="field__input" value={title} onChange={e => setTitle(e.target.value)} required />
                    </label>
                    <label className="field">
                      <div className="field__label">Company</div>
                      <input className="field__input" value={company} onChange={e => setCompany(e.target.value)} required />
                    </label>
                    <label className="field">
                      <div className="field__label">Location</div>
                      <input className="field__input" value={location} onChange={e => setLocation(e.target.value)} placeholder="Toronto, ON" />
                    </label>
                    <label className="field">
                      <div className="field__label">Dates</div>
                      <input className="field__input" value={dates} onChange={e => setDates(e.target.value)} placeholder="Jan 2025 - Present" />
                    </label>
                  </div>
                )}

                <label className="field">
                  <div className="field__label">{isExperience ? 'Short label (internal name)' : 'Name'}</div>
                  <input className="field__input" autoFocus value={name} onChange={e => setName(e.target.value)} required
                    placeholder={isExperience ? 'e.g. realty-investment-group' : 'e.g. resume-pipeline'} />
                </label>
                <label className="field">
                  <div className="field__label">Description / Bullet Bank Source</div>
                  <textarea className="field__textarea" value={description} onChange={e => setDescription(e.target.value)} required
                    style={{ minHeight: 200 }}
                    placeholder={isExperience
                      ? 'Describe what you built at this role, with what tech, at what scale. Use anchor numbers (e.g., "~64K listings", "~300ms latency"). Gemini will turn this into 6-12 bullets.'
                      : 'What does this project do? What did you build, with what tech, at what scale?'} />
                </label>
                <label className="field">
                  <div className="field__label">Local repo path (optional)</div>
                  <input className="field__input" value={sourcePath} onChange={e => setSourcePath(e.target.value)}
                    placeholder="C:\path\to\repo" />
                </label>
                {err && <div className="err">{err}</div>}
                <div className="row row--between">
                  <button type="button" className="btn btn--ghost" onClick={() => setShowForm(false)}>CANCEL</button>
                  <button type="submit" className="btn btn--acid" disabled={busy}>
                    {busy ? <span className="spinner">CREATING</span> : <>CREATE &nbsp;→</>}
                  </button>
                </div>
              </form>
            )}
          </div>
        </>
      )}
    </div>
  );
}
