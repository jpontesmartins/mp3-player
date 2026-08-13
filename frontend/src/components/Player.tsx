import { useRef, useCallback, useEffect, useState } from 'react';
import SkipNextIcon from '@mui/icons-material/SkipNext';
import SkipPreviousIcon from '@mui/icons-material/SkipPrevious';
import PauseIcon from '@mui/icons-material/Pause';
import StopIcon from '@mui/icons-material/Stop';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import type { Id3Tags } from '../App';

import { API } from '../config';

function formatTime(ms: number): string {
  if (!ms || ms <= 0) return '00:00:00';
  const total = Math.floor(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface Props {
  status: 'playing' | 'paused' | 'stopped';
  position: number;
  duration: number;
  currentFile: string | null;
  currentId3?: Id3Tags;
  showCover: boolean;
  coverUrl: string | null;
  onTogglePlayPause: () => void;
  onStop: () => void;
  onPrev: () => void;
  onNext: () => void;
  onSeek: (positionMs: number) => void;
  onScrollToCurrent: () => void;
}

function displayName(id3: Id3Tags | undefined): string {
  if (id3) {
    const a = id3.artist;
    const t = id3.title;
    if (a && t) return `${a} - ${t}`;
    if (t) return t;
  }
  return '';
}

export default function Player({ status, position, duration, currentFile, currentId3, showCover, coverUrl, onTogglePlayPause, onStop, onPrev, onNext, onSeek, onScrollToCurrent }: Props) {
  const barRef = useRef<HTMLDivElement>(null);
  const [coverBusy, setCoverBusy] = useState(false);
  const [coverMsg, setCoverMsg] = useState<string | null>(null);
  const [coverVersion, setCoverVersion] = useState(0);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number } | null>(null);
  const coverMsgTimer = useRef<number | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const pct = duration > 0 ? Math.min((position / duration) * 100, 100) : 0;
  const name = displayName(currentId3);
  const isPlaying = status === 'playing';
  const canToggle = !!currentFile;
  const canStop = isPlaying || status === 'paused';
  const canSkip = !!currentFile;
  const handleBarClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!duration || !currentFile || !barRef.current) return;
    const rect = barRef.current.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    onSeek(Math.round(ratio * duration));
  }, [duration, currentFile, onSeek]);

  const showCoverMsg = (msg: string) => {
    setCoverMsg(msg);
    if (coverMsgTimer.current !== null) {
      window.clearTimeout(coverMsgTimer.current);
    }
    coverMsgTimer.current = window.setTimeout(() => setCoverMsg(null), 4000);
  };

  const downloadCover = useCallback(async () => {
    if (!currentFile || coverBusy) return;
    setCoverBusy(true);
    showCoverMsg('Baixando capa...');
    try {
      const res = await fetch(`${API}/cover/download`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path: currentFile }),
      });
      const text = await res.text();
      if (!res.ok) {
        showCoverMsg(`Erro: ${text}`);
      } else {
        showCoverMsg('Capa baixada.');
        setCoverVersion(v => v + 1);
      }
    } catch {
      showCoverMsg('Erro ao conectar com o servidor');
    } finally {
      setCoverBusy(false);
    }
  }, [currentFile, coverBusy]);

  const handleCoverContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!currentFile) return;
    const menu = menuRef.current;
    let x = e.clientX + 4;
    let y = e.clientY + 4;
    if (menu) {
      const r = menu.getBoundingClientRect();
      if (x + r.width > window.innerWidth) x = e.clientX - r.width - 4;
      if (y + r.height > window.innerHeight) y = e.clientY - r.height - 4;
    }
    setMenuPos({ x: Math.max(4, x), y: Math.max(4, y) });
  }, [currentFile]);

  useEffect(() => {
    if (!menuPos) return undefined;
    const close = () => setMenuPos(null);
    window.addEventListener('mousedown', close);
    window.addEventListener('scroll', close, true);
    window.addEventListener('resize', close);
    return () => {
      window.removeEventListener('mousedown', close);
      window.removeEventListener('scroll', close, true);
      window.removeEventListener('resize', close);
    };
  }, [menuPos]);

  return (
    <section id="player-section">
      {showCover && currentFile && (
        <div
          id="cover-container"
          onContextMenu={handleCoverContextMenu}
          title="Clique com o botão direito para baixar a capa do álbum"
        >
          <img
            key={`${currentFile}:${coverVersion}`}
            id="album-cover"
            src={`${coverUrl!}${coverUrl!.includes('?') ? '&' : '?'}v=${coverVersion}`}
            alt="Capa do álbum"
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <span id="cover-placeholder">🎵</span>
          {coverMsg && <span id="cover-status">{coverMsg}</span>}
        </div>
      )}

      {menuPos && (
        <div
          ref={menuRef}
          id="cover-context-menu"
          style={{ left: menuPos.x, top: menuPos.y }}
          onMouseDown={e => e.stopPropagation()}
        >
          <button
            type="button"
            className="cover-menu-item"
            onClick={() => { setMenuPos(null); downloadCover(); }}
          >
            Baixar capa do álbum
          </button>
        </div>
      )}

      <div id="player-controls">
        <button id="prev-btn" onClick={onPrev} disabled={!canSkip}>
          <SkipPreviousIcon />
        </button>
        <button
          id="play-pause-btn"
          onClick={onTogglePlayPause}
          disabled={!canToggle}
        >
          {isPlaying ? <PauseIcon /> : <PlayArrowIcon />}
        </button>
        <button id="stop-btn" onClick={onStop} disabled={!canStop}>
          <StopIcon />
        </button>
        <button id="next-btn" onClick={onNext} disabled={!canSkip}>
          <SkipNextIcon />
        </button>
      </div>

      <div id="progress-section">
        <div id="progress-bar" ref={barRef} onClick={handleBarClick}>
          <div id="progress-fill" style={{ width: `${pct}%` }} />
        </div>
        <span id="time-display">
          {formatTime(position)} / {duration > 0 ? formatTime(duration) : '--:--:--'}
        </span>
      </div>

      <div id="status-section" style={{ fontSize: 14 }} onClick={onScrollToCurrent}>
        
        {status === 'playing' && `${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'paused' && `Pausado: ${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'stopped' && 'Nenhuma música tocando'}
      </div>
    </section>
  );
}
