import { useAuth } from '@oop/auth';
import { useCallback } from 'react';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export function useApiClient() {
  const { token } = useAuth();

  const apiFetch = useCallback(
    async (path: string, options: RequestInit = {}) => {
      const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...options.headers,
        },
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(`API error: ${res.status} - ${text}`);
      }
      return res.json();
    },
    [token]
  );

  const deployFiles = useCallback(
    async (files: File[]) => {
      const formData = new FormData();
      files.forEach((file) => formData.append('files', file));

      const res = await fetch(`${API_BASE}/api/v1/deploy`, {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      });
      if (!res.ok) {
        const text = await res.text();
        throw new Error(`Deploy failed: ${res.status} - ${text}`);
      }
      return res.json();
    },
    [token]
  );

  return { apiFetch, deployFiles };
}
