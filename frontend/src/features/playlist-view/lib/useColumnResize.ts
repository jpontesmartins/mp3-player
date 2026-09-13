import { useState, useRef, useEffect } from 'react';

type ResizeType = 'artist' | 'time';

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function useColumnResize(initialArtistPct = 30, initialTimePx = 62) {
  const [artistPct, setArtistPct] = useState(initialArtistPct);
  const [timePx, setTimePx] = useState(initialTimePx);
  const [dragType, setDragType] = useState<ResizeType | null>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const dragInfo = useRef<{ type: ResizeType; startX: number; startArtist: number; startTime: number } | null>(null);

  useEffect(() => {
    if (!dragType) return undefined;
    const onMove = (e: MouseEvent) => {
      const info = dragInfo.current;
      if (!info || !headerRef.current) return;
      const width = headerRef.current.clientWidth || 1;
      const dx = e.clientX - info.startX;
      if (info.type === 'artist') setArtistPct(clamp(info.startArtist + (dx / width) * 100, 15, 55));
      else setTimePx(clamp(info.startTime + dx, 40, 160));
    };
    const onUp = () => { dragInfo.current = null; setDragType(null); };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
  }, [dragType]);

  const startResize = (type: ResizeType) => (e: React.MouseEvent) => {
    e.preventDefault();
    dragInfo.current = { type, startX: e.clientX, startArtist: artistPct, startTime: timePx };
    setDragType(type);
  };

  const gridStyle = { gridTemplateColumns: `${artistPct}% 1fr ${timePx}px` };

  return { artistPct, timePx, dragType, headerRef, startResize, gridStyle };
}
