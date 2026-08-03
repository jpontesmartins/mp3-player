import { useEffect, useRef, useState } from 'react';
import type { Id3Tags } from '../App';

function formatTime(ms: number): string {
  if (!ms || ms <= 0) return '00:00:00';
  const total = Math.floor(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface Props {
  files: string[];
  currentFile: string | null;
  id3Cache: Map<string, Id3Tags>;
  onSelect: (file: string) => void;
}

function fileName(path: string): string {
  return path.split('\\').pop()!.split('/').pop()!;
}

function displayName(tags: Id3Tags | undefined, file: string): string {
  if (tags) {
    const artist = tags.artist;
    const title = tags.title;
    if (artist && title) return `${artist} - ${title}`;
    if (title) return title;
  }
  return fileName(file);
}

function totalDuration(files: string[], cache: Map<string, Id3Tags>): number {
  let total = 0;
  for (const f of files) {
    const d = cache.get(f)?.duration_ms;
    if (d) total += Number(d);
  }
  return total;
}

interface TooltipState {
  file: string;
  x: number;
  y: number;
}

const HOVER_DELAY_MS = 2000;

export default function Playlist({ files, currentFile, id3Cache, onSelect }: Props) {
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const [tooltipStyle, setTooltipStyle] = useState<{ left: number; top: number } | null>(null);
  const hoverTimer = useRef<number | null>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);

  const clearTimer = () => {
    if (hoverTimer.current !== null) {
      window.clearTimeout(hoverTimer.current);
      hoverTimer.current = null;
    }
  };

  const handleEnter = (file: string) => (e: React.MouseEvent<HTMLLIElement>) => {
    clearTimer();
    hoverTimer.current = window.setTimeout(() => {
      setTooltip({ file, x: e.clientX, y: e.clientY });
    }, HOVER_DELAY_MS);
  };

  const handleLeave = () => {
    clearTimer();
    setTooltip(null);
    setTooltipStyle(null);
  };

  useEffect(() => {
    if (!tooltip) {
      setTooltipStyle(null);
      return undefined;
    }
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

  if (files.length === 0) {
    return (
      <section id="playlist-section">
        <ul id="playlist">
          <li style={{ color: '#666' }}>Nenhum arquivo .mp3 encontrado</li>
        </ul>
      </section>
    );
  }

  const total = totalDuration(files, id3Cache);
  const tooltipTags = tooltip ? id3Cache.get(tooltip.file) : undefined;

  return (
    <section id="playlist-section">
      <ul id="playlist">
        {files.map((file) => {
          const tags = id3Cache.get(file);
          const name = displayName(tags, file);
          const dur = tags?.duration_ms ? Number(tags.duration_ms) : 0;
          const active = file === currentFile;
          return (
            <li
              key={file}
              className={active ? 'active' : ''}
              onClick={() => onSelect(file)}
              onMouseEnter={handleEnter(file)}
              onMouseLeave={handleLeave}
            >
              <span className="pl-name">{name}</span>
              {dur > 0 && <span className="pl-duration">{formatTime(dur)}</span>}
            </li>
          );
        })}
      </ul>
      <div id="playlist-footer">
        <span>{files.length} {files.length === 1 ? 'música' : 'músicas'}</span>
        {total > 0 && <span>{formatTime(total)}</span>}
      </div>

      {tooltip && tooltipTags && (
        <div
          ref={tooltipRef}
          className="id3-tooltip"
          style={tooltipStyle ?? { left: -9999, top: -9999 }}
        >
          <div className="id3-tooltip-title">
            {displayName(tooltipTags, fileName(tooltip.file))}
          </div>
          <dl className="id3-tooltip-list">
            {tooltipTags.artist && <div><dt>Artista</dt><dd>{tooltipTags.artist}</dd></div>}
            {tooltipTags.album && <div><dt>Álbum</dt><dd>{tooltipTags.album}</dd></div>}
            {tooltipTags.year && <div><dt>Ano</dt><dd>{tooltipTags.year}</dd></div>}
            {tooltipTags.genre && <div><dt>Gênero</dt><dd>{tooltipTags.genre}</dd></div>}
            {tooltipTags.track && <div><dt>Faixa</dt><dd>{tooltipTags.track}</dd></div>}
            {tooltipTags.duration_ms && <div><dt>Duração</dt><dd>{formatTime(Number(tooltipTags.duration_ms))}</dd></div>}
            {tooltipTags.kbps && <div><dt>Bitrate</dt><dd>{tooltipTags.kbps} kbps</dd></div>}
          </dl>
        </div>
      )}
    </section>
  );
}
