export const API = 'http://localhost:8111';

export async function fetchJSON<T>(url: string, init?: RequestInit): Promise<T | null> {
  try {
    const res = await fetch(url, init);
    if (res.ok) return await res.json() as T;
    return null;
  } catch {
    return null;
  }
}

export async function fetchText(url: string, init?: RequestInit): Promise<{ ok: boolean; text: string }> {
  try {
    const res = await fetch(url, init);
    const text = await res.text();
    return { ok: res.ok, text };
  } catch {
    return { ok: false, text: 'Erro ao conectar com o servidor' };
  }
}

export async function postJSON(url: string, body: unknown): Promise<boolean> {
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return res.ok;
  } catch {
    return false;
  }
}

export async function postText(url: string, body: string): Promise<boolean> {
  try {
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body,
    });
    return res.ok;
  } catch {
    return false;
  }
}

export async function deleteResource(url: string): Promise<boolean> {
  try {
    const res = await fetch(url, { method: 'DELETE' });
    return res.ok;
  } catch {
    return false;
  }
}
