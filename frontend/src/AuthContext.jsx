import { createContext, useContext, useMemo, useState } from 'react';
import api from './api';
import { clearCart } from './cart';

const AuthCtx = createContext(null);

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('ch_user') || 'null');
  } catch {
    return null;
  }
}

function persist(token, user, setUser) {
  localStorage.setItem('ch_token', token);
  localStorage.setItem('ch_user', JSON.stringify(user));
  clearCart();
  setUser(user);
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const value = useMemo(
    () => ({
      user,
      async login(email, password) {
        const r = await api.post('/auth/login', { email, password });
        persist(r.data.token, r.data.user, setUser);
        return r.data.user;
      },
      async register(payload) {
        const r = await api.post('/auth/register', payload);
        persist(r.data.token, r.data.user, setUser);
        return r.data.user;
      },
      logout() {
        localStorage.removeItem('ch_token');
        localStorage.removeItem('ch_user');
        clearCart();
        setUser(null);
      },
    }),
    [user]
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth() {
  return useContext(AuthCtx);
}
