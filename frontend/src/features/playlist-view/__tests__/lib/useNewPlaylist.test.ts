import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useNewPlaylist } from '../../lib/useNewPlaylist';
import { listPlaylists, savePlaylist } from '../../../../shared/api/playlist';

vi.mock('../../../../shared/api/playlist', () => ({
  listPlaylists: vi.fn(),
  savePlaylist: vi.fn(),
}));

const FILE = 'C:\\music\\a.mp3';

function setup(existing: string[] = []) {
  const opts = { file: FILE, existing, refresh: vi.fn().mockResolvedValue(undefined), onCreated: vi.fn(), onClose: vi.fn() };
  const hook = renderHook(() => useNewPlaylist(opts));
  return { ...hook, opts };
}

describe('useNewPlaylist', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listPlaylists).mockResolvedValue([]);
    vi.mocked(savePlaylist).mockResolvedValue(true);
  });

  it('canSave is false for empty or blank name', () => {
    const { result } = setup();
    expect(result.current.canSave).toBe(false);
    act(() => result.current.setName('   '));
    expect(result.current.canSave).toBe(false);
  });

  it('blocks duplicate names', async () => {
    vi.mocked(listPlaylists).mockResolvedValue(['Rock']);
    const { result, opts } = setup();
    act(() => result.current.setName('rock'));
    await act(() => result.current.submit());
    expect(result.current.error).toBe('Já existe uma playlist com esse nome');
    expect(savePlaylist).not.toHaveBeenCalled();
    expect(opts.onClose).not.toHaveBeenCalled();
    expect(result.current.saving).toBe(false);
  });

  it('falls back to existing list when listPlaylists fails', async () => {
    vi.mocked(listPlaylists).mockResolvedValue(null);
    const { result } = setup(['Rock']);
    act(() => result.current.setName('Rock'));
    await act(() => result.current.submit());
    expect(result.current.error).toBe('Já existe uma playlist com esse nome');
    expect(savePlaylist).not.toHaveBeenCalled();
  });

  it('saves trimmed name with the clicked file and closes', async () => {
    const { result, opts } = setup();
    act(() => result.current.setName('  Viagem  '));
    await act(() => result.current.submit());
    expect(savePlaylist).toHaveBeenCalledWith('Viagem', [FILE]);
    expect(opts.refresh).toHaveBeenCalled();
    expect(opts.onCreated).toHaveBeenCalledWith('Viagem');
    expect(opts.onClose).toHaveBeenCalled();
  });

  it('shows error and stays open when save fails', async () => {
    vi.mocked(savePlaylist).mockResolvedValue(false);
    const { result, opts } = setup();
    act(() => result.current.setName('Viagem'));
    await act(() => result.current.submit());
    expect(result.current.error).toBe('Erro ao salvar playlist');
    expect(result.current.saving).toBe(false);
    expect(opts.onClose).not.toHaveBeenCalled();
  });

  it('setName clears error', async () => {
    vi.mocked(savePlaylist).mockResolvedValue(false);
    const { result } = setup();
    act(() => result.current.setName('Viagem'));
    await act(() => result.current.submit());
    act(() => result.current.setName('Viagem 2'));
    expect(result.current.error).toBe('');
  });
});
