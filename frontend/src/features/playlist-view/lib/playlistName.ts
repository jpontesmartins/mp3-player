/**
 * Espelha `FilePlaylistRepository.sanitize` (backend): trim, troca `[\/:*?"<>|]` por `_`;
 * resultado vazio vira `playlist`.
 */
export function sanitizePlaylistName(name: string): string {
  const cleaned = name.trim().replace(/[\\/:*?"<>|]/g, '_');
  return cleaned === '' ? 'playlist' : cleaned;
}

/** Indica se o nome (sanitizado, sem diferenciar caixa) coincide com alguma playlist existente. */
export function isDuplicatePlaylistName(name: string, existing: string[]): boolean {
  const target = sanitizePlaylistName(name).toLowerCase();
  return existing.some(item => sanitizePlaylistName(item).toLowerCase() === target);
}
