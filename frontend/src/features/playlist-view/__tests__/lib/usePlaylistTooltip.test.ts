import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { usePlaylistTooltip } from '../../lib/usePlaylistTooltip';

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('usePlaylistTooltip', () => {
  it('returns initial state', () => {
    const { result } = renderHook(() => usePlaylistTooltip());

    expect(result.current.tooltip).toBeNull();
    expect(result.current.tooltipStyle).toBeNull();
    expect(result.current.tooltipRef).toBeDefined();
  });

  it('handleEnter sets tooltip after 1 second', () => {
    const { result } = renderHook(() => usePlaylistTooltip());

    act(() => {
      const enterHandler = result.current.handleEnter('song.mp3');
      enterHandler({
        clientX: 100,
        clientY: 200,
      } as React.MouseEvent<HTMLLIElement>);
    });

    expect(result.current.tooltip).toBeNull();

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(result.current.tooltip).toEqual({ file: 'song.mp3', x: 100, y: 200 });
  });

  it('handleLeave clears tooltip', () => {
    const { result } = renderHook(() => usePlaylistTooltip());

    act(() => {
      const enterHandler = result.current.handleEnter('song.mp3');
      enterHandler({
        clientX: 100,
        clientY: 200,
      } as React.MouseEvent<HTMLLIElement>);
    });

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(result.current.tooltip).not.toBeNull();

    act(() => {
      result.current.handleLeave();
    });

    expect(result.current.tooltip).toBeNull();
    expect(result.current.tooltipStyle).toBeNull();
  });

  it('handleLeave cancels pending timer', () => {
    const { result } = renderHook(() => usePlaylistTooltip());

    act(() => {
      const enterHandler = result.current.handleEnter('song.mp3');
      enterHandler({
        clientX: 100,
        clientY: 200,
      } as React.MouseEvent<HTMLLIElement>);
    });

    act(() => {
      result.current.handleLeave();
    });

    act(() => {
      vi.advanceTimersByTime(1000);
    });

    expect(result.current.tooltip).toBeNull();
  });
});
