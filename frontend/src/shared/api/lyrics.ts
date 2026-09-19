import { API, fetchText } from './client';

export const getCached = (path: string) =>
  fetchText(`${API}/lyrics/cached?path=${encodeURIComponent(path)}`);

export const get = (path: string) =>
  fetchText(`${API}/lyrics?path=${encodeURIComponent(path)}`);

export async function save(path: string, text: string): Promise<boolean> {
  try {
    const response = await fetch(`${API}/lyrics`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path, text }),
    });
    return response.ok;
  } catch {
    return false;
  }
}

export const remove = (path: string) =>
  fetchText(`${API}/lyrics?path=${encodeURIComponent(path)}`, { method: 'DELETE' });
