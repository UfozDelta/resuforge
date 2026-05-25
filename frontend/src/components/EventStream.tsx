import { useEffect, useRef, useSyncExternalStore } from 'react';
import { API_BASE } from '../lib/api';

interface Props {
  jdText: string;
  jdUrl: string;
  roleEmphasis: string;
  onDone: (appId: string) => void;
  onClose: () => void;
}

function lineColor(line: string): string {
  const l = line.toLowerCase();
  if (l.startsWith('cut:')) return '#c05000';
  if (l.startsWith('pdf compile failed') || l.startsWith('tectonic:')) return '#c00';
  if (l.startsWith('kept:') || l.startsWith('saved ') || l.startsWith('done -')) return '#2a7a2a';
  if (l.startsWith('selection complete') || l.startsWith('  ')) return '#2a7a2a';
  if (l.startsWith('ats matched')) return '#2a7a2a';
  if (l.startsWith('retry result:') && l.includes('passed')) return '#2a7a2a';
  if (l.startsWith('ats missing')) return '#c05000';
  return '#8a6800';
}

/**
 * Modal popup that streams SSE pipeline events.
 *
 * Uses useSyncExternalStore to bypass React 18 concurrent scheduler batching.
 * setState inside async microtask chains gets deferred until the promise settles.
 * useSyncExternalStore forces a synchronous re-render on every notify() call,
 * so each SSE line paints immediately as bytes arrive.
 */
export function EventStream({ jdText, jdUrl, roleEmphasis, onDone, onClose }: Props) {
  // External store — lives outside React's scheduler
  const linesRef = useRef<string[]>([]);
  const statusRef = useRef<{ error: string | null; done: boolean }>({ error: null, done: false });
  const subsRef = useRef(new Set<() => void>());
  const bottomRef = useRef<HTMLDivElement>(null);

  function notify() {
    subsRef.current.forEach(cb => cb());
  }

  const lines = useSyncExternalStore(
    cb => { subsRef.current.add(cb); return () => subsRef.current.delete(cb); },
    () => linesRef.current,
  );
  const status = useSyncExternalStore(
    cb => { subsRef.current.add(cb); return () => subsRef.current.delete(cb); },
    () => statusRef.current,
  );

  useEffect(() => {
    const ctrl = new AbortController();

    async function run() {
      try {
        const res = await fetch(`${API_BASE}/api/applications/stream`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            jdText: jdText.trim() || undefined,
            jdUrl: jdUrl.trim() || undefined,
            roleEmphasis,
          }),
          signal: ctrl.signal,
        });

        if (!res.ok || !res.body) {
          const text = await res.text();
          statusRef.current = { error: text || `HTTP ${res.status}`, done: false };
          notify();
          return;
        }

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buf = '';
        let currentEvent = 'log';

        while (true) {
          const { done: streamDone, value } = await reader.read();
          if (streamDone) break;

          buf += decoder.decode(value, { stream: true });
          const rawLines = buf.split('\n');
          buf = rawLines.pop() ?? '';

          for (const raw of rawLines) {
            const line = raw.trimEnd();
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              const data = line.slice(5).trim();
              if (currentEvent === 'log') {
                // Mutate then notify — useSyncExternalStore forces synchronous re-render
                linesRef.current = [...linesRef.current, data];
                notify();
              } else if (currentEvent === 'done') {
                statusRef.current = { error: null, done: true };
                notify();
                setTimeout(() => onDone(data), 600);
                return;
              } else if (currentEvent === 'error') {
                statusRef.current = { error: data, done: false };
                notify();
                return;
              }
              currentEvent = 'log';
            }
          }
        }

        statusRef.current = { error: 'Stream ended without done event', done: false };
        notify();
      } catch (e: any) {
        if (e?.name !== 'AbortError') {
          statusRef.current = { error: e?.message || 'Stream error', done: false };
          notify();
        }
      }
    }

    run();
    return () => ctrl.abort();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [lines]);

  const { error, done } = status;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 1000,
        background: 'rgba(10,10,10,0.72)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
      }}
      onClick={e => { if (e.target === e.currentTarget && (done || !!error)) onClose(); }}
    >
      <div
        style={{
          background: 'var(--paper)',
          border: '2px solid var(--ink)',
          width: '100%',
          maxWidth: 680,
          maxHeight: '80vh',
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
        }}
      >
        {/* Header */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            borderBottom: '2px solid var(--ink)',
            background: 'var(--ink)',
            color: 'var(--paper)',
          }}
        >
          <span style={{ fontFamily: 'var(--mono)', fontSize: 11, letterSpacing: '0.18em', textTransform: 'uppercase' }}>
            {done ? 'PIPELINE COMPLETE' : error ? 'PIPELINE ERROR' : 'TAILORING RESUME...'}
          </span>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--paper)',
              cursor: 'pointer',
              fontFamily: 'var(--mono)',
              fontSize: 13,
              padding: '0 4px',
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        </div>

        {/* Log body */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '12px 16px',
            fontFamily: 'var(--mono)',
            fontSize: 11,
            lineHeight: 1.8,
          }}
        >
          {lines.length === 0 && !error && (
            <div style={{ color: 'var(--muted)' }}>Connecting...</div>
          )}
          {lines.map((line, i) => (
            <div key={i} style={{ color: lineColor(line) }}>{line}</div>
          ))}
          {error && (
            <div style={{ color: '#c00', marginTop: 8 }}>{error}</div>
          )}
          {!done && !error && lines.length > 0 && (
            <div style={{ color: '#888', marginTop: 4 }}>...</div>
          )}
          <div ref={bottomRef} />
        </div>

        {/* Footer */}
        {(done || !!error) && (
          <div
            style={{
              padding: '12px 16px',
              borderTop: '2px solid var(--ink)',
              display: 'flex',
              justifyContent: 'flex-end',
              gap: 8,
            }}
          >
            <button className="btn btn--ghost btn--sm" onClick={onClose}>
              CLOSE
            </button>
            {done && (
              <span style={{ fontFamily: 'var(--mono)', fontSize: 11, color: '#2a7a2a', alignSelf: 'center' }}>
                Redirecting...
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
