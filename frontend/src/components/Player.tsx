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
  onPlay: () => void;
  onPause: () => void;
  onResume: () => void;
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

export default function Player({ status, position, duration, currentFile, currentId3, onPlay, onPause, onResume }: Props) {
  const pct = duration > 0 ? Math.min((position / duration) * 100, 100) : 0;
  const name = displayName(currentId3);

  return (
    <section id="player-section">
      <div id="player-controls">
        <button id="play-btn" onClick={onPlay} disabled={status === 'playing' || status === 'paused' || !currentFile}>
          ▶
        </button>
        <button id="pause-btn" onClick={onPause} disabled={status !== 'playing'}>
          ⏸
        </button>
        <button id="resume-btn" onClick={onResume} disabled={status !== 'paused'}>
          ▶
        </button>
      </div>

      <div id="progress-section">
        <div id="progress-bar">
          <div id="progress-fill" style={{ width: `${pct}%` }} />
        </div>
        <span id="time-display">
          {formatTime(position)} / {duration > 0 ? formatTime(duration) : '--:--:--'}
        </span>
      </div>

      <div id="status-section">
        {status === 'playing' && `Tocando: ${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'paused' && `Pausado: ${name || currentFile?.split('\\').pop()?.split('/').pop() || ''}`}
        {status === 'stopped' && 'Nenhuma música tocando'}
      </div>
    </section>
  );
}
