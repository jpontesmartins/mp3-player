import { API } from './client';
import type { Id3Tags } from '../types';

export async function bulkId3(files: string[], refresh = false): Promise<Record<string, Id3Tags> | null> {
  try {
    const response = await fetch(`${API}/id3/bulk?refresh=${refresh}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(files),
    });
    if (response.ok) return await response.json() as Record<string, Id3Tags>;
    return null;
  } catch {
    return null;
  }
}

export async function updateId3(path: string, tags: Record<string, string>): Promise<Id3Tags | null> {
  try {
    const response = await fetch(`${API}/id3/update`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ path, tags }),
    });
    if (response.ok) return await response.json() as Id3Tags;
    return null;
  } catch {
    return null;
  }
}
