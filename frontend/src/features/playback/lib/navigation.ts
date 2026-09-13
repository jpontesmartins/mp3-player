import type { PlaybackMode } from '../../../shared/types';

export function getNextFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[0];
  const idx = files.indexOf(current);
  if (idx < 0 || idx >= files.length - 1) return files[0];
  return files[idx + 1];
}

export function getPrevFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[files.length - 1];
  const idx = files.indexOf(current);
  if (idx <= 0) return files[files.length - 1];
  return files[idx - 1];
}
