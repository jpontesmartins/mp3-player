import { useState, useRef, useCallback } from 'react';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { revealItemInDir } from '@tauri-apps/plugin-opener';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { usePlayback } from '../../playback/lib/usePlayback';
import { formatTime, fileName } from '../../../shared/lib/format';
import { ContextMenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';
import type { Id3Tags } from '../../../shared/types';
import { useColumnResize } from '../lib/useColumnResize';
import { usePlaylistTooltip } from '../lib/usePlaylistTooltip';
import { usePlaylistSearch } from '../lib/usePlaylistSearch';
import ColumnHeader from './ColumnHeader';
import PlaylistTooltip from './PlaylistTooltip';
import SearchBar from './SearchBar';

function totalDuration(files: string[], cache: Map<string, Id3Tags>): number {
  let total = 0;
  for (const file of files) {
    const durationMilliseconds = cache.get(file)?.duration_ms;
    if (durationMilliseconds) total += Number(durationMilliseconds);
  }
  return total;
}

export default function Playlist() {
  const player = usePlayer();
  const library = useLibrary();
  const { playFile } = usePlayback();

  const resize = useColumnResize();
  const tooltipHook = usePlaylistTooltip();
  const search = usePlaylistSearch();

  const [songContextMenu, setSongContextMenu] = useState<{ x: number; y: number; file: string } | null>(null);
  const songContextMenuRef = useRef<HTMLDivElement>(null);

  const tooltipTags = tooltipHook.tooltip ? library.id3Cache.get(tooltipHook.tooltip.file) : undefined;

  useContextMenuClose(!!songContextMenu, () => setSongContextMenu(null));

  const handleSongContextMenu = useCallback((e: React.MouseEvent<HTMLLIElement>, file: string) => {
    e.preventDefault();
    e.stopPropagation();
    setSongContextMenu({ ...getContextMenuPosition(e, songContextMenuRef.current), file });
  }, []);

  const openFolderForSong = useCallback(async (file: string) => { setSongContextMenu(null); try { await revealItemInDir(file); } catch { /* noop */ } }, []);
  const copySongPath = useCallback((file: string) => { setSongContextMenu(null); navigator.clipboard.writeText(file).catch(() => {}); }, []);

  const displayFiles = search.filteredFiles;
  const total = totalDuration(displayFiles, library.id3Cache);

  if (library.id3Loading) {
    return (
      <>
        <section id="playlist-section">
          <ColumnHeader {...resize} />
          <div className="playlist-loading">
            <AutorenewIcon className="playlist-spinner" />
            <span>Carregando dados... {library.id3Total > 0 ? `${library.id3Loaded}/${library.id3Total}` : ''}</span>
          </div>
        </section>
        <SearchBar {...search} filteredCount={displayFiles.length} totalCount={library.playlistFiles.length} />
      </>
    );
  }

  if (library.playlistFiles.length === 0) {
    return (
      <>
        <section id="playlist-section">
          <ul id="playlist"><li style={{ color: '#666' }}>Nenhum arquivo .mp3 encontrado</li></ul>
        </section>
        <SearchBar {...search} filteredCount={0} totalCount={0} disabled />
      </>
    );
  }

  return (
    <>
      <section id="playlist-section">
        <ColumnHeader {...resize} />
        <ul id="playlist">
          {displayFiles.map((file) => {
            const tags = library.id3Cache.get(file);
            const artist = tags?.artist || '';
            const title = tags?.title || fileName(file);
            const durationMilliseconds = tags?.duration_ms ? Number(tags.duration_ms) : 0;
            const active = file === player.currentFile;
            return (
              <li key={file} className={active ? 'active' : ''} style={resize.gridStyle} onClick={() => playFile(file)} onContextMenu={e => handleSongContextMenu(e, file)} onMouseEnter={tooltipHook.handleEnter(file)} onMouseLeave={tooltipHook.handleLeave}>
                <span className="pl-artist">{artist}</span>
                <span className="pl-title">{title}</span>
                <span className="pl-duration">{durationMilliseconds > 0 ? formatTime(durationMilliseconds) : ''}</span>
              </li>
            );
          })}
        </ul>
        <div id="playlist-footer">
          <span>{displayFiles.length} {displayFiles.length === 1 ? 'música' : 'músicas'}</span>
          {total > 0 && <span>{formatTime(total)}</span>}
        </div>

        {tooltipHook.tooltip && tooltipTags && (
          <PlaylistTooltip
            file={tooltipHook.tooltip.file}
            x={tooltipHook.tooltip.x}
            y={tooltipHook.tooltip.y}
            style={tooltipHook.tooltipStyle}
            ref={tooltipHook.tooltipRef}
            tags={tooltipTags}
          />
        )}
      </section>

      <SearchBar {...search} filteredCount={displayFiles.length} totalCount={library.playlistFiles.length} />

      {songContextMenu && (
        <div ref={songContextMenuRef} id="song-context-menu" style={{ left: songContextMenu.x, top: songContextMenu.y }} onMouseDown={e => e.stopPropagation()}>
          <ContextMenuItem icon={<FolderOpenIcon />} label="Abrir pasta no explorer" onClick={() => openFolderForSong(songContextMenu.file)} />
          <ContextMenuItem icon={<ContentCopyIcon />} label="Copiar caminho" onClick={() => copySongPath(songContextMenu.file)} />
        </div>
      )}
    </>
  );
}
