import { Link } from 'react-router-dom';
import '../styles/landing.css';

const STEPS = [
  {
    num: '01',
    title: 'Build your\nbullet bank',
    body: 'Add projects and experiences. Paste a description or point at a local repo. Gemini Pro writes 6–12 bullets per entry — edit freely.',
    tag: 'GEMINI PRO',
  },
  {
    num: '02',
    title: 'Paste the\njob posting',
    body: 'Drop in raw JD text or a URL. Flash cleans it, extracts company, role, and keywords. Takes two seconds.',
    tag: 'GEMINI FLASH',
  },
  {
    num: '03',
    title: 'AI ranks\nevery bullet',
    body: 'Pro scores every bullet against the JD. Top 8 auto-selected (max 3 per project). Override anything. See exactly why each bullet ranked.',
    tag: 'RANKED MATCHING',
  },
  {
    num: '04',
    title: 'One-click\nPDF output',
    body: 'LaTeX template → tectonic compiler → production-ready PDF stored in the database. Cover letter included.',
    tag: 'LATEX + TECTONIC',
  },
];

const FEATURES = [
  {
    icon: '⬡',
    title: 'Structured output',
    body: 'Every LLM call uses enforced JSON schemas. No hallucinated shapes, no string parsing. Swap the client for Claude or OpenAI by changing one class.',
  },
  {
    icon: '◈',
    title: 'Application history',
    body: 'Track every application from sent to offer or rejection. Filter by outcome. Store the full ranked bullet list and PDF alongside each application.',
  },
  {
    icon: '◻',
    title: 'ATS gap analysis',
    body: 'See matched and missing keywords for each application before you submit. Cover letter tailored to the same JD.',
  },
];

const STACK = [
  ['Backend',    'Java 21 · Spring Boot 3 · Maven'],
  ['Database',   'Neon Postgres · Flyway migrations'],
  ['LLM',        'Gemini 2.5 Pro + Flash · AI Studio'],
  ['PDF',        'LaTeX template · tectonic compiler'],
  ['Frontend',   'Vite · React · TypeScript'],
  ['Deploy',     'Render (Docker) · Vercel · Neon'],
];

export function Landing() {
  return (
    <div className="lp-root">

      {/* ── HERO ── */}
      <section className="lp-hero shell">
        <div className="lp-hero__eyebrow">
          <span className="lp-label">VOL.0 / ISSUE.1</span>
          <div className="lp-hero__rule" />
          <span className="lp-label lp-muted">RESUME TAILORING ATELIER</span>
        </div>

        <div className="lp-hero__grid">
          <div className="lp-hero__left">
            <h1 className="lp-display lp-hero__heading">
              Resume<br />
              <span className="lp-hero__slash">// </span>
              Pipeline
            </h1>
            <p className="lp-editorial lp-hero__sub">
              Paste a job description.<br />
              Get a tailored résumé PDF<br />
              in under thirty seconds.
            </p>
            <div className="lp-hero__cta-row">
              <Link to="/login" className="lp-btn lp-btn--acid">
                ENTER THE ATELIER &nbsp;→
              </Link>
              <span className="lp-label lp-muted lp-hero__cta-note">
                SINGLE-USER · SESSION AUTH
              </span>
            </div>
          </div>

          <div className="lp-hero__right">
            <div className="lp-hero__terminal">
              <div className="lp-hero__terminal-bar">
                <span />
                <span />
                <span />
                <span className="lp-hero__terminal-title">resume-pipeline — tectonic</span>
              </div>
              <pre className="lp-hero__terminal-body">{`$ POST /api/applications
  jdUrl: "https://jobs.example.com/..."
  roleEmphasis: "distributed systems"

← 200 OK  (18.4s)
  company:      "Acme Corp"
  role:         "Senior Engineer"
  bullets:      8 selected / 34 ranked
  ats_matched:  ["Kubernetes","gRPC","Postgres"]
  ats_missing:  ["Terraform"]
  pdf_blob:     264 KB
  cover_letter: ✓`}</pre>
            </div>
          </div>
        </div>

        <div className="lp-hero__ticker">
          <span>AI BULLET GENERATION</span>
          <span className="lp-ticker-sep">——</span>
          <span>JD KEYWORD MATCHING</span>
          <span className="lp-ticker-sep">——</span>
          <span>LATEX PDF RENDERING</span>
          <span className="lp-ticker-sep">——</span>
          <span>APPLICATION HISTORY</span>
          <span className="lp-ticker-sep">——</span>
          <span>ATS GAP ANALYSIS</span>
          <span className="lp-ticker-sep">——</span>
          <span>COVER LETTER</span>
          <span className="lp-ticker-sep">——</span>
        </div>
      </section>

      {/* ── HOW IT WORKS ── */}
      <section className="lp-steps shell">
        <div className="lp-section-mark">
          <span className="lp-label lp-muted">§ A</span>
          <div className="lp-section-rule" />
          <span className="lp-section-title">HOW IT WORKS</span>
        </div>

        <div className="lp-steps__grid">
          {STEPS.map((s) => (
            <div key={s.num} className="lp-step">
              <div className="lp-step__num">{s.num}</div>
              <h3 className="lp-display lp-step__title">{s.title}</h3>
              <p className="lp-step__body">{s.body}</p>
              <div className="lp-tag">{s.tag}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section className="lp-features shell">
        <div className="lp-section-mark">
          <span className="lp-label lp-muted">§ B</span>
          <div className="lp-section-rule" />
          <span className="lp-section-title">BUILT TO LAST</span>
        </div>

        <div className="lp-features__grid">
          {FEATURES.map((f) => (
            <div key={f.title} className="lp-feature">
              <div className="lp-feature__icon">{f.icon}</div>
              <h4 className="lp-feature__title">{f.title}</h4>
              <p className="lp-feature__body">{f.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── STACK ── */}
      <section className="lp-stack shell">
        <div className="lp-section-mark">
          <span className="lp-label lp-muted">§ C</span>
          <div className="lp-section-rule" />
          <span className="lp-section-title">STACK</span>
        </div>

        <div className="lp-stack__table">
          {STACK.map(([layer, detail]) => (
            <div key={layer} className="lp-stack__row">
              <span className="lp-stack__layer">{layer}</span>
              <div className="lp-stack__dots" />
              <span className="lp-stack__detail">{detail}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA BAND ── */}
      <section className="lp-cta-band">
        <div className="shell lp-cta-band__inner">
          <p className="lp-display lp-cta-band__heading">
            One user.<br />One résumé.<br />One job at a time.
          </p>
          <Link to="/login" className="lp-btn lp-btn--ink">
            OPEN PIPELINE &nbsp;→
          </Link>
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer className="lp-footer shell">
        <span className="lp-label lp-muted">RESUME // PIPELINE</span>
        <span className="lp-label lp-muted">SPRING BOOT · REACT · NEON · GEMINI · TECTONIC</span>
      </footer>

    </div>
  );
}
