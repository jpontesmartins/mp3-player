import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { usePlayback } from '../../lib/usePlayback';
import { PlayerProvider } from '../../../../app/providers/PlayerContext';
import { LibraryProvider } from '../../../../app/providers/LibraryContext';

vi.mock('../../../../shared/api/playback', () => ({
  play: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  stop: vi.fn(),
  seek: vi.fn(),
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
  vi.clearAllMocks();
});

describe('usePlayback', () => {
  describe('playFile', () => {
    it('sets currentFile and status to playing on success', async () => {
      vi.mocked(playbackApi.play).mockResolvedValue(true);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.playFile('song.mp3'));

      expect(playbackApi.play).toHaveBeenCalledWith('song.mp3');
    });

    it('does nothing on API failure', async () => {
      vi.mocked(playbackApi.play).mockResolvedValue(false);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.playFile('song.mp3'));

      expect(playbackApi.play).toHaveBeenCalledWith('song.mp3');
    });
  });

  describe('pause', () => {
    it('calls pause API', async () => {
      vi.mocked(playbackApi.pause).mockResolvedValue(true);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.pause());

      expect(playbackApi.pause).toHaveBeenCalled();
    });
  });

  describe('resume', () => {
    it('calls resume API', async () => {
      vi.mocked(playbackApi.resume).mockResolvedValue(true);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.resume());

      expect(playbackApi.resume).toHaveBeenCalled();
    });
  });

  describe('stop', () => {
    it('calls stop API and sets intentionalStop', async () => {
      vi.mocked(playbackApi.stop).mockResolvedValue(true);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.stop());

      expect(playbackApi.stop).toHaveBeenCalled();
    });
  });

  describe('seek', () => {
    it('calls seek API with position', async () => {
      vi.mocked(playbackApi.seek).mockResolvedValue(true);
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.seek(5000));

      expect(playbackApi.seek).toHaveBeenCalledWith(5000);
    });
  });

  describe('togglePlayPause', () => {
    it('does nothing when stopped and no currentFile', async () => {
      const { result } = renderHook(() => usePlayback(), { wrapper });

      await act(() => result.current.togglePlayPause());

      expect(playbackApi.play).not.toHaveBeenCalled();
      expect(playbackApi.pause).not.toHaveBeenCalled();
      expect(playbackApi.resume).not.toHaveBeenCalled();
    });
  });

  describe('scrollToCurrent', () => {
    it('does not throw when no active element exists', () => {
      const { result } = renderHook(() => usePlayback(), { wrapper });
      expect(() => result.current.scrollToCurrent()).not.toThrow();
    });
  });
});
