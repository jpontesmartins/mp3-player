import { API } from './client';

export const getCoverUrl = (path: string) =>
  `${API}/cover?path=${encodeURIComponent(path)}`;

export async function downloadCover(path: string): Promise<{ ok: boolean; text: string }> {
  try {
    const response = await fetch(`${API}/cover/download`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path }),
    });
    const text = await response.text();
    return { ok: response.ok, text };
  } catch {
    return { ok: false, text: 'Erro ao conectar com o servidor' };
  }
}
