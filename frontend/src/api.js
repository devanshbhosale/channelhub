import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('ch_token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      localStorage.removeItem('ch_token');
      localStorage.removeItem('ch_user');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login');
      }
    }
    return Promise.reject(err);
  }
);

// Extracts the API's `{error: "..."}` message verbatim when available.
export function apiError(err) {
  const data = err && err.response && err.response.data;
  if (data) {
    if (typeof data === 'string') return data;
    if (data.error) return data.error;
    if (data.message) return data.message;
  }
  return (err && err.message) || 'Request failed';
}

export default api;
