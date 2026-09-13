import type { PlaybackMode } from '../../../shared/types';

export function getNextFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[0];
  const index = files.indexOf(current);
  if (index < 0 || index >= files.length - 1) return files[0];
  return files[index + 1];
}

export function getPreviousFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[files.length - 1];
  const index = files.indexOf(current);
  if (index <= 0) return files[files.length - 1];
  return files[index - 1];
}
