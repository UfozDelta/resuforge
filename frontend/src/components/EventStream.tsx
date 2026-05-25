import { useEffect, useRef, useSyncExternalStore } from 'react';
import { API_BASE } from '../lib/api';

interface Props {
  jdText: string;
  jdUrl: string;
  roleEmphasis: string;
  onDone: (appId: string) => void;
  onClose: () => void;
}

interface StreamState {
  lines: string[];
  error: string | null;
  done: boolean;
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
 * Single useSyncExternalStore returning one snapshot object.
 * Two separate hooks sharing subsRef caused React to batch both
 * notifications into one render cycle, defeating synchronous rendering.
 * One store → one notify → one forced synchronous commit per SSE line.
 */
export function EventStream({ jdText, jdUrl, roleEmphasis, onDone, onClose }: Props) {
  const storeRef = useRef<StreamState>({ lines: [], error: null, done: false });
  const subsRef = useRef(new Set<() => void>());
  const bottomRef = useRef<HTMLDivElement>(null);
  const onDoneRef = useRef(onDone);
  onDoneRef.current = onDone;

  function notify() {
    subsRef.current.forEach(cb => cb());
  }

  const state = useSyncExternalStore(
    cb => { subsRef.current.add(cb); return () => subsRef.current.delete(cb); },
    () => storeRef.current,
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
          storeRef.current = { ...storeRef.current, error: text || `HTTP ${res.status}` };
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
                // New object reference required — useSyncExternalStore uses Object.is()
                storeRef.current = { ...storeRef.current, lines: [...storeRef.current.lines, data] };
                notify();
              } else if (currentEvent === 'done') {
                storeRef.current = { ...storeRef.current, done: true };
                notify();
                setTimeout(() => onDoneRef.current(data), 600);
                return;
              } else if (currentEvent === 'error') {
                storeRef.current = { ...storeRef.current, error: data };
                notify();
                return;
              }
              currentEvent = 'log';
            }
          }
        }

        storeRef.current = { ...storeRef.current, error: 'Stream ended without done event' };
        notify();
      } catch (e: any) {
        if (e?.name !== 'AbortError') {
          storeRef.current = { ...storeRef.current, error: e?.message || 'Stream error' };
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
  }, [state.lines]);

  const { lines, error, done } = state;

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
