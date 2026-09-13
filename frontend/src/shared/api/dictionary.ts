import { API, fetchJSON } from './client';
import type { DictionaryResult } from '../types';

export const lookup = (word: string, language: string) =>
  fetchJSON<DictionaryResult>(`${API}/dictionary/lookup`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ word, language }),
  });
