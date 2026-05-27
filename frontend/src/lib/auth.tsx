import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { api, UnauthorizedError } from './api';

interface AuthState {
  username: string | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [username, setUsername] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.get<{ username: string }>('/api/me')
      .then(r => setUsername(r.username))
      .catch(() => setUsername(null))
      .finally(() => setLoading(false));
  }, []);

  const login = async (u: string, p: string) => {
    const r = await api.post<{ username: string }>('/api/login', { username: u, password: p });
    setUsername(r.username);
  };

  const logout = async () => {
    try { await api.post('/api/logout'); } catch { /* ignore */ }
    setUsername(null);
  };

  const register = async (u: string, email: string, p: string) => {
    const r = await api.post<{ username: string }>('/api/register', { username: u, email, password: p });
    setUsername(r.username);
  };

  return (
    <AuthContext.Provider value={{ username, loading, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth outside provider');
  return ctx;
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { username, loading } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  useEffect(() => {
    if (!loading && !username) nav('/login', { replace: true, state: { from: loc.pathname } });
  }, [loading, username, nav, loc.pathname]);
  if (loading) return <div className="center-page"><span className="spinner">LOADING</span></div>;
  if (!username) return null;
  return <>{children}</>;
}

export { UnauthorizedError };
