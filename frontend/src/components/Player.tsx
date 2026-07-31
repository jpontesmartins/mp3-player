import { useRef, useCallback } from 'react';
import SkipNextIcon from '@mui/icons-material/SkipNext';
import SkipPreviousIcon from '@mui/icons-material/SkipPrevious';
import PauseIcon from '@mui/icons-material/Pause';
import StopIcon from '@mui/icons-material/Stop';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
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

export default function Player({ status, position, duration, currentFile, currentId3, showCover, coverUrl, onTogglePlayPause, onStop, onPrev, onNext, onSeek }: Props) {
  const barRef = useRef<HTMLDivElement>(null);
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

  return (
    <section id="player-section">
      {showCover && currentFile && (
        <div id="cover-container">
          <img
            key={currentFile}
            id="album-cover"
            src={coverUrl!}
            alt="Capa do álbum"
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <span id="cover-placeholder">🎵</span>
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

      <div id="status-section" style={{ fontSize: 14 }}>
        {status === 'playing' && `${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'paused' && `Pausado: ${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'stopped' && 'Nenhuma música tocando'}
      </div>
    </section>
  );
}
