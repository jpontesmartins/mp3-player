import { describe, it, expect } from 'vitest';
import { getNextFile, getPrevFile } from '../../lib/navigation';

const files = ['a.mp3', 'b.mp3', 'c.mp3', 'd.mp3'];

describe('getNextFile', () => {
  it('returns null for empty list', () => {
    expect(getNextFile(null, [], 'continuous')).toBeNull();
  });

  it('returns current in repeat mode', () => {
    expect(getNextFile('b.mp3', files, 'repeat')).toBe('b.mp3');
  });

  it('returns a file from the list in shuffle mode', () => {
    const result = getNextFile('a.mp3', files, 'shuffle');
    expect(files).toContain(result);
  });

  it('returns first file when current is null', () => {
    expect(getNextFile(null, files, 'continuous')).toBe('a.mp3');
  });

  it('returns next file in sequence', () => {
    expect(getNextFile('a.mp3', files, 'continuous')).toBe('b.mp3');
    expect(getNextFile('b.mp3', files, 'continuous')).toBe('c.mp3');
  });

  it('wraps to first file after last', () => {
    expect(getNextFile('d.mp3', files, 'continuous')).toBe('a.mp3');
  });

  it('wraps to first file when current not found', () => {
    expect(getNextFile('z.mp3', files, 'continuous')).toBe('a.mp3');
  });
});

describe('getPrevFile', () => {
  it('returns null for empty list', () => {
    expect(getPrevFile(null, [], 'continuous')).toBeNull();
  });

  it('returns current in repeat mode', () => {
    expect(getPrevFile('b.mp3', files, 'repeat')).toBe('b.mp3');
  });

  it('returns a file from the list in shuffle mode', () => {
    const result = getPrevFile('a.mp3', files, 'shuffle');
    expect(files).toContain(result);
  });

  it('returns last file when current is null', () => {
    expect(getPrevFile(null, files, 'continuous')).toBe('d.mp3');
  });

  it('returns previous file in sequence', () => {
    expect(getPrevFile('c.mp3', files, 'continuous')).toBe('b.mp3');
    expect(getPrevFile('b.mp3', files, 'continuous')).toBe('a.mp3');
  });

  it('wraps to last file before first', () => {
    expect(getPrevFile('a.mp3', files, 'continuous')).toBe('d.mp3');
  });

  it('wraps to last file when current not found', () => {
    expect(getPrevFile('z.mp3', files, 'continuous')).toBe('d.mp3');
  });
});
