import { useRef, useCallback } from 'react';
import SkipNextIcon from '@mui/icons-material/SkipNext';
import SkipPreviousIcon from '@mui/icons-material/SkipPrevious';
import PauseIcon from '@mui/icons-material/Pause';
import StopIcon from '@mui/icons-material/Stop';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { usePlayback } from '../lib/usePlayback';
import { formatTime, displayName } from '../../../shared/lib/format';
import CoverArt from './CoverArt';

export default function Player() {
  const player = usePlayer();
  const library = useLibrary();
  const { togglePlayPause, stop, previous, next, seek, scrollToCurrent } = usePlayback();

  const barRef = useRef<HTMLDivElement>(null);

  const progressPercentage = player.duration > 0 ? Math.min((player.position / player.duration) * 100, 100) : 0;
  const currentId3 = player.currentFile ? library.id3Cache.get(player.currentFile) : undefined;
  const name = displayName(currentId3, '');
  const isPlaying = player.status === 'playing';
  const canToggle = !!player.currentFile;
  const canStop = isPlaying || player.status === 'paused';
  const canSkip = !!player.currentFile;

  const handleBarClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!player.duration || !player.currentFile || !barRef.current) return;
    const rect = barRef.current.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    seek(Math.round(ratio * player.duration));
  }, [player.duration, player.currentFile, seek]);

  const fallbackName = player.currentFile
    ? player.currentFile?.split('\\').pop()?.split('/').pop() || ''
    : '';

  return (
    <section id="player-section">
      <CoverArt currentFile={player.currentFile} showCover={player.showCover} />

      <div id="player-controls">
        <button id="prev-btn" onClick={previous} disabled={!canSkip}><SkipPreviousIcon /></button>
        <button id="play-pause-btn" onClick={togglePlayPause} disabled={!canToggle}>
          {isPlaying ? <PauseIcon /> : <PlayArrowIcon />}
        </button>
        <button id="stop-btn" onClick={stop} disabled={!canStop}><StopIcon /></button>
        <button id="next-btn" onClick={next} disabled={!canSkip}><SkipNextIcon /></button>
      </div>

      <div id="progress-section">
        <div id="progress-bar" ref={barRef} onClick={handleBarClick}>
          <div id="progress-fill" style={{ width: `${progressPercentage}%` }} />
        </div>
        <span id="time-display">
          {formatTime(player.position)} / {player.duration > 0 ? formatTime(player.duration) : '--:--:--'}
        </span>
      </div>

      <div id="status-section" style={{ fontSize: 14 }} onClick={scrollToCurrent}>
        {player.status === 'playing' && (name || fallbackName)}
        {player.status === 'paused' && `Pausado: ${name || fallbackName}`}
        {player.status === 'stopped' && 'Nenhuma música tocando'}
      </div>
    </section>
  );
}
