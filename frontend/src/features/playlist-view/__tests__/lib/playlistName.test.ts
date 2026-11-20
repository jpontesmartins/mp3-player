import { describe, it, expect } from 'vitest';
import { sanitizePlaylistName, isDuplicatePlaylistName } from '../../lib/playlistName';

describe('sanitizePlaylistName', () => {
  it('trims', () => expect(sanitizePlaylistName('  Rock  ')).toBe('Rock'));
  it('replaces invalid characters', () => expect(sanitizePlaylistName('a\\b/c:d*e?f"g<h>i|j')).toBe('a_b_c_d_e_f_g_h_i_j'));
  it('falls back to "playlist" when empty', () => {
    expect(sanitizePlaylistName('')).toBe('playlist');
    expect(sanitizePlaylistName('   ')).toBe('playlist');
  });
});

describe('isDuplicatePlaylistName', () => {
  it('matches exact name', () => expect(isDuplicatePlaylistName('Rock', ['Rock'])).toBe(true));
  it('ignores case', () => expect(isDuplicatePlaylistName('rock', ['Rock'])).toBe(true));
  it('compares sanitized names', () => expect(isDuplicatePlaylistName('a/b', ['a_b'])).toBe(true));
  it('returns false for new name', () => expect(isDuplicatePlaylistName('Pop', ['Rock'])).toBe(false));
  it('returns false for empty list', () => expect(isDuplicatePlaylistName('Pop', [])).toBe(false));
});
