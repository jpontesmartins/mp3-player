import type { Id3Tags } from '../types';

export function formatTime(milliseconds: number): string {
  if (!milliseconds || milliseconds <= 0) return '00:00:00';
  const total = Math.floor(milliseconds / 1000);
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const seconds = total % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

export function fileName(path: string): string {
  return path.split('\\').pop()!.split('/').pop()!;
}

export function parentDirectory(path: string): string {
  const index = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
  return index < 0 ? path : path.substring(0, index);
}

export function displayName(tags: Id3Tags | undefined, file: string): string {
  if (tags) {
    const artist = tags.artist;
    const title = tags.title;
    if (artist && title) return `${artist} - ${title}`;
    if (title) return title;
  }
  return fileName(file);
}

export function escapeRegex(inputString: string): string {
  return inputString.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
