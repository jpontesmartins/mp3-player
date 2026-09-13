import { useState, useRef, useEffect } from 'react';

type ResizeType = 'artist' | 'time';

function clamp(value: number, minimum: number, maximum: number): number {
  return Math.min(maximum, Math.max(minimum, value));
}

export function useColumnResize(initialArtistPercentage = 30, initialTimePixels = 62) {
  const [artistPercentage, setArtistPercentage] = useState(initialArtistPercentage);
  const [timePixels, setTimePixels] = useState(initialTimePixels);
  const [dragType, setDragType] = useState<ResizeType | null>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const dragInformation = useRef<{ type: ResizeType; startX: number; startArtist: number; startTime: number } | null>(null);

  useEffect(() => {
    if (!dragType) return undefined;
    const onMove = (e: MouseEvent) => {
      const information = dragInformation.current;
      if (!information || !headerRef.current) return;
      const width = headerRef.current.clientWidth || 1;
      const deltaX = e.clientX - information.startX;
      if (information.type === 'artist') setArtistPercentage(clamp(information.startArtist + (deltaX / width) * 100, 15, 55));
      else setTimePixels(clamp(information.startTime + deltaX, 40, 160));
    };
    const onUp = () => { dragInformation.current = null; setDragType(null); };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); };
  }, [dragType]);

  const startResize = (type: ResizeType) => (e: React.MouseEvent) => {
    e.preventDefault();
    dragInformation.current = { type, startX: e.clientX, startArtist: artistPercentage, startTime: timePixels };
    setDragType(type);
  };

  const gridStyle = { gridTemplateColumns: `${artistPercentage}% 1fr ${timePixels}px` };

  return { artistPercentage, timePixels, dragType, headerRef, startResize, gridStyle };
}
