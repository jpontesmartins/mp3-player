import { useState, useRef, useEffect, useCallback } from 'react';

interface TooltipState { file: string; x: number; y: number; }

export function usePlaylistTooltip() {
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const [tooltipStyle, setTooltipStyle] = useState<{ left: number; top: number } | null>(null);
  const hoverTimer = useRef<number | null>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  const clearTimer = useCallback(() => {
    if (hoverTimer.current !== null) {
      window.clearTimeout(hoverTimer.current);
      hoverTimer.current = null;
    }
  }, []);

  const handleEnter = useCallback((file: string) => (e: React.MouseEvent<HTMLLIElement>) => {
    clearTimer();
    hoverTimer.current = window.setTimeout(() => setTooltip({ file, x: e.clientX, y: e.clientY }), 1000);
  }, [clearTimer]);

  const handleLeave = useCallback(() => {
    clearTimer();
    setTooltip(null);
    setTooltipStyle(null);
  }, [clearTimer]);

  useEffect(() => {
    if (!tooltip) { setTooltipStyle(null); return undefined; }
    let raf = 0;
    raf = requestAnimationFrame(() => {
      const el = tooltipRef.current;
      if (!el) return;
      const r = el.getBoundingClientRect();
      let left = tooltip.x + 14;
      let top = tooltip.y + 14;
      if (left + r.width > window.innerWidth) left = tooltip.x - r.width - 14;
      if (top + r.height > window.innerHeight) top = tooltip.y - r.height - 14;
      setTooltipStyle({ left: Math.max(6, left), top: Math.max(6, top) });
    });
    return () => cancelAnimationFrame(raf);
  }, [tooltip]);

  return { tooltip, tooltipStyle, tooltipRef, handleEnter, handleLeave };
}
