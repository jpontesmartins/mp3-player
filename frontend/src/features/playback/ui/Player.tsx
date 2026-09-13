import { useRef, useCallback, useState } from 'react';
import SkipNextIcon from '@mui/icons-material/SkipNext';
import SkipPreviousIcon from '@mui/icons-material/SkipPrevious';
import PauseIcon from '@mui/icons-material/Pause';
import StopIcon from '@mui/icons-material/Stop';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { usePlayback } from '../lib/usePlayback';
import { formatTime, displayName } from '../../../shared/lib/format';
import { getCoverUrl, downloadCover } from '../../../shared/api/cover';
import { CtxMenuItem, getContextMenuPosition } from '../../../shared/ui/ContextMenu';
import { useContextMenuClose } from '../../../shared/ui/ContextMenu';

export default function Player() {
  const player = usePlayer();
  const library = useLibrary();
  const { togglePlayPause, stop, prev, next, seek, scrollToCurrent } = usePlayback();
  
  const barRef = useRef<HTMLDivElement>(null);
  const [coverBusy, setCoverBusy] = useState(false);
  const [coverMsg, setCoverMsg] = useState<string | null>(null);
  const [coverVersion, setCoverVersion] = useState(0);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number } | null>(null);
  const coverMsgTimer = useRef<number | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const pct = player.duration > 0 ? Math.min((player.position / player.duration) * 100, 100) : 0;
  const currentId3 = player.currentFile ? library.id3Cache.get(player.currentFile) : undefined;
  const name = displayName(currentId3, '');
  const isPlaying = player.status === 'playing';
  const canToggle = !!player.currentFile;
  const canStop = isPlaying || player.status === 'paused';
  const canSkip = !!player.currentFile;
  const coverUrl = player.currentFile ? getCoverUrl(player.currentFile) : null;

  const handleBarClick = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!player.duration || !player.currentFile || !barRef.current) return;
    const rect = barRef.current.getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    seek(Math.round(ratio * player.duration));
  }, [player.duration, player.currentFile, seek]);

  const showCoverMsg = (msg: string) => {
    setCoverMsg(msg);
    if (coverMsgTimer.current !== null) window.clearTimeout(coverMsgTimer.current);
    coverMsgTimer.current = window.setTimeout(() => setCoverMsg(null), 4000);
  };

  const handleDownloadCover = useCallback(async () => {
    if (!player.currentFile || coverBusy) return;
    setCoverBusy(true);
    showCoverMsg('Baixando capa...');
    const result = await downloadCover(player.currentFile);
    showCoverMsg(result.ok ? 'Capa baixada.' : `Erro: ${result.text}`);
    if (result.ok) setCoverVersion(v => v + 1);
    setCoverBusy(false);
  }, [player.currentFile, coverBusy]);

  const handleCoverContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!player.currentFile) return;
    setMenuPos(getContextMenuPosition(e, menuRef.current));
  }, [player.currentFile]);

  useContextMenuClose(!!menuPos, () => setMenuPos(null));

  const fallbackName = player.currentFile
    ? player.currentFile?.split('\\').pop()?.split('/').pop() || ''
    : '';

  return (
    <section id="player-section">
      {player.showCover && player.currentFile && (
        <div id="cover-container" onContextMenu={handleCoverContextMenu} title="Clique com o botão direito para baixar a capa do álbum">
          <img
            key={`${player.currentFile}:${coverVersion}`}
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
        <div ref={menuRef} id="cover-context-menu" style={{ left: menuPos.x, top: menuPos.y }} onMouseDown={e => e.stopPropagation()}>
          <CtxMenuItem icon={<CloudDownloadIcon />} label="Baixar capa do álbum" onClick={() => { setMenuPos(null); handleDownloadCover(); }} />
        </div>
      )}

      <div id="player-controls">
        <button id="prev-btn" onClick={prev} disabled={!canSkip}><SkipPreviousIcon /></button>
        <button id="play-pause-btn" onClick={togglePlayPause} disabled={!canToggle}>
          {isPlaying ? <PauseIcon /> : <PlayArrowIcon />}
        </button>
        <button id="stop-btn" onClick={stop} disabled={!canStop}><StopIcon /></button>
        <button id="next-btn" onClick={next} disabled={!canSkip}><SkipNextIcon /></button>
      </div>

      <div id="progress-section">
        <div id="progress-bar" ref={barRef} onClick={handleBarClick}>
          <div id="progress-fill" style={{ width: `${pct}%` }} />
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
