export const API = 'http://localhost:8111';

export async function fetchJSON<T>(url: string, init?: RequestInit): Promise<T | null> {
  try {
    const response = await fetch(url, init);
    if (response.ok) return await response.json() as T;
    return null;
  } catch {
    return null;
  }
}

export async function fetchText(url: string, init?: RequestInit): Promise<{ ok: boolean; text: string }> {
  try {
    const response = await fetch(url, init);
    const text = await response.text();
    return { ok: response.ok, text };
  } catch {
    return { ok: false, text: 'Erro ao conectar com o servidor' };
  }
}

export async function postJSON(url: string, body: unknown): Promise<boolean> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    return response.ok;
  } catch {
    return false;
  }
}

export async function postText(url: string, body: string): Promise<boolean> {
  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body,
    });
    return response.ok;
  } catch {
    return false;
  }
}

export async function deleteResource(url: string): Promise<boolean> {
  try {
    const response = await fetch(url, { method: 'DELETE' });
    return response.ok;
  } catch {
    return false;
  }
}
