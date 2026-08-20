import { useEffect, useRef, useState, useMemo, useCallback } from 'react';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import SearchIcon from '@mui/icons-material/Search';
import SaveIcon from '@mui/icons-material/Save';
import type { Id3Tags } from '../App';
import { filterPlaylist } from '../searchParser';

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
  loading?: boolean;
  id3Loaded?: number;
  id3Total?: number;
  onSelect: (file: string) => void;
  onSaveFiltered?: (files: string[], name: string) => Promise<boolean>;
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

const HOVER_DELAY_MS = 1000;

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

type ResizeType = 'artist' | 'time';

export default function Playlist({ files, currentFile, id3Cache, loading = false, id3Loaded = 0, id3Total = 0, onSelect, onSaveFiltered }: Props) {
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const [tooltipStyle, setTooltipStyle] = useState<{ left: number; top: number } | null>(null);
  const [artistPct, setArtistPct] = useState(30);
  const [timePx, setTimePx] = useState(62);
  const [dragType, setDragType] = useState<ResizeType | null>(null);
  const [query, setQuery] = useState('');
  const [searchFocused, setSearchFocused] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [saveOpen, setSaveOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveMsg, setSaveMsg] = useState('');
  const hoverTimer = useRef<number | null>(null);
  const tooltipRef = useRef<HTMLDivElement>(null);
  const headerRef = useRef<HTMLDivElement>(null);
  const dragInfo = useRef<{ type: ResizeType; startX: number; startArtist: number; startTime: number } | null>(null);
  const textareaRef = useRef<HTMLInputElement>(null);
  const saveInputRef = useRef<HTMLInputElement>(null);
  const saveMsgTimer = useRef<number | null>(null);

  const filteredFiles = useMemo(
    () => filterPlaylist(files, query, id3Cache),
    [files, query, id3Cache],
  );

  const displayFiles = filteredFiles;
  const isFiltered = query.trim().length > 0 && filteredFiles.length < files.length;

  useEffect(() => {
    if (!dragType) return undefined;
    const onMove = (e: MouseEvent) => {
      const info = dragInfo.current;
      if (!info || !headerRef.current) return;
      const width = headerRef.current.clientWidth || 1;
      const dx = e.clientX - info.startX;
      if (info.type === 'artist') {
        setArtistPct(clamp(info.startArtist + (dx / width) * 100, 15, 55));
      } else {
        setTimePx(clamp(info.startTime + dx, 40, 160));
      }
    };
    const onUp = () => {
      dragInfo.current = null;
      setDragType(null);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
  }, [dragType]);

  const startResize = (type: ResizeType) => (e: React.MouseEvent) => {
    e.preventDefault();
    dragInfo.current = { type, startX: e.clientX, startArtist: artistPct, startTime: timePx };
    setDragType(type);
  };

  const gridStyle = { gridTemplateColumns: `${artistPct}% 1fr ${timePx}px` };
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

  useEffect(() => {
    if (saveOpen && saveInputRef.current) {
      saveInputRef.current.focus();
    }
  }, [saveOpen]);

  useEffect(() => {
    return () => {
      if (saveMsgTimer.current !== null) window.clearTimeout(saveMsgTimer.current);
    };
  }, []);

  const handleSaveConfirm = useCallback(async () => {
    const name = saveName.trim();
    if (!name || !onSaveFiltered || filteredFiles.length === 0) return;
    setSaving(true);
    try {
      const ok = await onSaveFiltered(filteredFiles, name);
      if (ok) {
        setSaveMsg(`Playlist "${name}" salva (${filteredFiles.length} músicas)`);
        setSaveOpen(false);
        setSaveName('');
      } else {
        setSaveMsg('Erro ao salvar playlist');
      }
    } catch {
      setSaveMsg('Erro de conexão');
    } finally {
      setSaving(false);
      if (saveMsgTimer.current !== null) window.clearTimeout(saveMsgTimer.current);
      saveMsgTimer.current = window.setTimeout(() => setSaveMsg(''), 4000);
    }
  }, [saveName, onSaveFiltered, filteredFiles]);

  const searchExpanded = searchFocused || query.trim().length > 0;

  if (loading) {
    return (
      <>
        <section id="playlist-section">
          <div id="playlist-header" style={gridStyle}>
            <span className="pl-header-artist">Artista</span>
            <span className="pl-header-title">Música</span>
            <span className="pl-header-time">Tempo</span>
          </div>
          <div className="playlist-loading">
            <AutorenewIcon className="playlist-spinner" />
            <span>Carregando dados... {id3Total > 0 ? `${id3Loaded}/${id3Total}` : ''}</span>
          </div>
        </section>
        <div id="playlist-search-bar" className={searchExpanded ? 'expanded' : ''}>
          <span className="search-icon"><SearchIcon /></span>
          <input
            ref={textareaRef}
            className="playlist-search-input"
            placeholder='Buscar: Nirvana || Pearl Jam | <genre> == grunge | <year> == 1991'
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            onBlur={() => setSearchFocused(false)}
            onKeyDown={e => {
              if (e.key === 'Escape') {
                setQuery('');
                textareaRef.current?.blur();
              }
            }}
          />
        </div>
      </>
    );
  }

  if (files.length === 0) {
    return (
      <>
        <section id="playlist-section">
          <ul id="playlist">
            <li style={{ color: '#666' }}>Nenhum arquivo .mp3 encontrado</li>
          </ul>
        </section>
        <div id="playlist-search-bar">
          <span className="search-icon"><SearchIcon /></span>
          <input
            className="playlist-search-input"
            placeholder='Buscar: Nirvana || Pearl Jam | <genre> == grunge | <year> == 1991'
            type="text"
            disabled
          />
        </div>
      </>
    );
  }

  const total = totalDuration(displayFiles, id3Cache);
  const tooltipTags = tooltip ? id3Cache.get(tooltip.file) : undefined;

  return (
    <>
      <section id="playlist-section">
        <div id="playlist-header" ref={headerRef} style={gridStyle}>
          <span className="pl-header-artist">Artista</span>
          <span className="pl-header-title">Música</span>
          <span className="pl-header-time">Tempo</span>
          <span
            className={`pl-resizer ${dragType === 'artist' ? 'dragging' : ''}`}
            style={{ left: `calc(${artistPct}% - 4px)` }}
            title="Redimensionar coluna"
            onMouseDown={startResize('artist')}
          />
          <span
            className={`pl-resizer ${dragType === 'time' ? 'dragging' : ''}`}
            style={{ left: `calc(100% - ${timePx}px - 5px)` }}
            title="Redimensionar coluna"
            onMouseDown={startResize('time')}
          />
        </div>
        <ul id="playlist">
          {displayFiles.map((file) => {
            const tags = id3Cache.get(file);
            const artist = tags?.artist || '';
            const title = tags?.title || fileName(file);
            const dur = tags?.duration_ms ? Number(tags.duration_ms) : 0;
            const active = file === currentFile;
            return (
              <li
                key={file}
                className={active ? 'active' : ''}
                style={gridStyle}
                onClick={() => onSelect(file)}
                onMouseEnter={handleEnter(file)}
                onMouseLeave={handleLeave}
              >
                <span className="pl-artist">{artist}</span>
                <span className="pl-title">{title}</span>
                <span className="pl-duration">{dur > 0 ? formatTime(dur) : ''}</span>
              </li>
            );
          })}
        </ul>
        <div id="playlist-footer">
          <span>{displayFiles.length} {displayFiles.length === 1 ? 'música' : 'músicas'}</span>
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
              {tooltipTags.title && <div><dt>Música</dt><dd>{tooltipTags.title}</dd></div>}
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

      <div id="playlist-search-bar" className={searchExpanded ? 'expanded' : ''}>
        <span className="search-icon"><SearchIcon /></span>
        <div className="playlist-search-field">
          <input
            ref={textareaRef}
            className="playlist-search-input"
            placeholder='Buscar: Nirvana || Pearl Jam | <genre> == grunge | <year> == 1991'
            type="text"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onFocus={() => setSearchFocused(true)}
            onBlur={() => setSearchFocused(false)}
            onKeyDown={e => {
              if (e.key === 'Escape') {
                setQuery('');
                textareaRef.current?.blur();
              }
            }}
          />
          {isFiltered && (
            <span className="playlist-search-count">
              {filteredFiles.length} / {files.length}
            </span>
          )}
        </div>
        {isFiltered && onSaveFiltered && (
          <button
            className="playlist-save-btn"
            title="Salvar resultado como playlist"
            onClick={() => { setSaveOpen(true); setSaveName(''); }}
          >
            <SaveIcon />
          </button>
        )}
      </div>

      {saveOpen && (
        <div className="playlist-save-row">
          <input
            ref={saveInputRef}
            className="playlist-save-input"
            placeholder="Nome da playlist"
            value={saveName}
            onChange={e => setSaveName(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') handleSaveConfirm();
              if (e.key === 'Escape') setSaveOpen(false);
            }}
          />
          <button
            className="pmanager-btn primary"
            onClick={handleSaveConfirm}
            disabled={saving || !saveName.trim()}
          >
            {saving ? '...' : 'Salvar'}
          </button>
          <button className="pmanager-btn" onClick={() => setSaveOpen(false)}>Cancelar</button>
        </div>
      )}

      {saveMsg && <div className="playlist-save-msg">{saveMsg}</div>}

    </>
  );
}
