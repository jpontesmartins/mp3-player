import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useColumnResize } from '../../lib/useColumnResize';

describe('useColumnResize', () => {
  it('returns default values', () => {
    const { result } = renderHook(() => useColumnResize());

    expect(result.current.artistPercentage).toBe(30);
    expect(result.current.timePixels).toBe(62);
    expect(result.current.dragType).toBeNull();
    expect(result.current.gridStyle.gridTemplateColumns).toBe('30% 1fr 62px');
  });

  it('accepts custom initial values', () => {
    const { result } = renderHook(() => useColumnResize(40, 80));

    expect(result.current.artistPercentage).toBe(40);
    expect(result.current.timePixels).toBe(80);
    expect(result.current.gridStyle.gridTemplateColumns).toBe('40% 1fr 80px');
  });

  it('startResize sets dragType', () => {
    const { result } = renderHook(() => useColumnResize());

    act(() => {
      const mouseDown = result.current.startResize('artist');
      mouseDown({ preventDefault: () => {} } as React.MouseEvent);
    });

    expect(result.current.dragType).toBe('artist');
  });

  it('startResize for time sets dragType to time', () => {
    const { result } = renderHook(() => useColumnResize());

    act(() => {
      const mouseDown = result.current.startResize('time');
      mouseDown({ preventDefault: () => {} } as React.MouseEvent);
    });

    expect(result.current.dragType).toBe('time');
  });

  it('headerRef is defined', () => {
    const { result } = renderHook(() => useColumnResize());

    expect(result.current.headerRef).toBeDefined();
  });
});
