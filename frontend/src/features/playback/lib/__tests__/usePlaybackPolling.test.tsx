import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { usePlaybackPolling } from '../usePlaybackPolling';
import { PlayerProvider } from '../../../../app/providers/PlayerContext';
import { LibraryProvider } from '../../../../app/providers/LibraryContext';

vi.mock('../../../../shared/api/playback', () => ({
  getStatus: vi.fn(),
  play: vi.fn(),
}));

import * as playbackApi from '../../../../shared/api/playback';

function wrapper({ children }: { children: ReactNode }) {
  return (
    <LibraryProvider>
      <PlayerProvider>{children}</PlayerProvider>
    </LibraryProvider>
  );
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.mocked(playbackApi.getStatus).mockResolvedValue(null);
});

afterEach(() => {
  vi.useRealTimers();
  vi.clearAllMocks();
});

describe('usePlaybackPolling', () => {
  it('calls getStatus immediately on mount and every 2s after', async () => {
    renderHook(() => usePlaybackPolling(), { wrapper });

    await act(() => vi.advanceTimersByTimeAsync(0));
    expect(playbackApi.getStatus).toHaveBeenCalledTimes(1);

    await act(() => vi.advanceTimersByTimeAsync(2000));
    expect(playbackApi.getStatus).toHaveBeenCalledTimes(2);

    await act(() => vi.advanceTimersByTimeAsync(2000));
    expect(playbackApi.getStatus).toHaveBeenCalledTimes(3);
  });

  it('handles stopped status with auto-advance', async () => {
    vi.mocked(playbackApi.getStatus)
      .mockResolvedValueOnce({ status: 'playing', file: 'a.mp3', position: 0, duration: 100 })
      .mockResolvedValueOnce({ status: 'stopped', file: 'a.mp3', position: 0, duration: 0 });
    vi.mocked(playbackApi.play).mockResolvedValue(true);

    renderHook(() => usePlaybackPolling(), { wrapper });

    await act(() => vi.advanceTimersByTimeAsync(0));
    await act(() => vi.advanceTimersByTimeAsync(2000));

    expect(playbackApi.getStatus).toHaveBeenCalledTimes(2);
  });

  it('handles playing status by updating position and duration', async () => {
    vi.mocked(playbackApi.getStatus).mockResolvedValue({
      status: 'playing',
      file: 'a.mp3',
      position: 5000,
      duration: 10000,
    });

    renderHook(() => usePlaybackPolling(), { wrapper });

    await act(() => vi.advanceTimersByTimeAsync(0));

    expect(playbackApi.getStatus).toHaveBeenCalledTimes(1);
  });

  it('handles null response gracefully', async () => {
    vi.mocked(playbackApi.getStatus).mockResolvedValue(null);

    renderHook(() => usePlaybackPolling(), { wrapper });

    await act(() => vi.advanceTimersByTimeAsync(0));

    expect(playbackApi.getStatus).toHaveBeenCalledTimes(1);
  });

  it('cleans up on unmount', async () => {
    const { unmount } = renderHook(() => usePlaybackPolling(), { wrapper });

    await act(() => vi.advanceTimersByTimeAsync(0));
    expect(playbackApi.getStatus).toHaveBeenCalledTimes(1);

    unmount();

    await act(() => vi.advanceTimersByTimeAsync(2000));
    expect(playbackApi.getStatus).toHaveBeenCalledTimes(1);
  });
});
