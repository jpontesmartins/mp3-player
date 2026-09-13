import { API, postText } from './client';

export const play = (file: string) => postText(`${API}/play`, file);
export const pause = () => postText(`${API}/pause`, '');
export const resume = () => postText(`${API}/resume`, '');
export const stop = () => postText(`${API}/stop`, '');
export const seek = (positionMs: number) =>
  fetch(`${API}/seek`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ position: positionMs }),
  }).then(r => r.ok).catch(() => false);

import type { PlayingData } from '../types';
import { fetchJSON } from './client';

export const getStatus = () => fetchJSON<PlayingData>(`${API}/playing`);
