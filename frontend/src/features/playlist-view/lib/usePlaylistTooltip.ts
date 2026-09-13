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
    let animationFrameId = 0;
    animationFrameId = requestAnimationFrame(() => {
      const tooltipElement = tooltipRef.current;
      if (!tooltipElement) return;
      const boundingRect = tooltipElement.getBoundingClientRect();
      let left = tooltip.x + 14;
      let top = tooltip.y + 14;
      if (left + boundingRect.width > window.innerWidth) left = tooltip.x - boundingRect.width - 14;
      if (top + boundingRect.height > window.innerHeight) top = tooltip.y - boundingRect.height - 14;
      setTooltipStyle({ left: Math.max(6, left), top: Math.max(6, top) });
    });
    return () => cancelAnimationFrame(animationFrameId);
  }, [tooltip]);

  return { tooltip, tooltipStyle, tooltipRef, handleEnter, handleLeave };
}
