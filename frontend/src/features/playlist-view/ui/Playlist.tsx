import { useState, useRef, useCallback, useEffect } from 'react';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd';
import { revealItemInDir } from '@tauri-apps/plugin-opener';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { usePlayback } from '../../playback/lib/usePlayback';
import { formatTime, fileName } from '../../../shared/lib/format';
import { ContextMenuItem, ContextMenuSubmenu, ContextMenuSubmenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';
import * as playlistApi from '../../../shared/api/playlist';
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
  const [addMessage, setAddMessage] = useState('');
  const addMessageTimer = useRef<number | null>(null);

  const tooltipTags = tooltipHook.tooltip ? library.id3Cache.get(tooltipHook.tooltip.file) : undefined;

  useContextMenuClose(!!songContextMenu, () => setSongContextMenu(null));

  useEffect(() => () => {
    if (addMessageTimer.current !== null) window.clearTimeout(addMessageTimer.current);
  }, []);

  const showAddMessage = useCallback((message: string) => {
    setAddMessage(message);
    if (addMessageTimer.current !== null) window.clearTimeout(addMessageTimer.current);
    addMessageTimer.current = window.setTimeout(() => setAddMessage(''), 4000);
  }, []);

  const handleSongContextMenu = useCallback((e: React.MouseEvent<HTMLLIElement>, file: string) => {
    e.preventDefault();
    e.stopPropagation();
    setSongContextMenu({ ...getContextMenuPosition(e, songContextMenuRef.current), file });
    library.refreshPlaylists();
  }, [library]);

  const openFolderForSong = useCallback(async (file: string) => { setSongContextMenu(null); try { await revealItemInDir(file); } catch { /* noop */ } }, []);
  const copySongPath = useCallback((file: string) => { setSongContextMenu(null); navigator.clipboard.writeText(file).catch(() => {}); }, []);

  const addToPlaylist = useCallback(async (name: string, file: string) => {
    setSongContextMenu(null);
    const paths = await playlistApi.loadVirtual(name);
    if (!paths) {
      showAddMessage(`Erro ao carregar "${name}"`);
      return;
    }
    if (paths.includes(file)) {
      showAddMessage(`"${fileName(file)}" já está em "${name}"`);
      return;
    }
    const success = await playlistApi.savePlaylist(name, [...paths, file]);
    showAddMessage(success ? `"${fileName(file)}" adicionada a "${name}"` : 'Erro ao salvar playlist');
  }, [showAddMessage]);

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
          <div className="ctx-menu-separator" />
          <ContextMenuSubmenu icon={<PlaylistAddIcon />} label="Adicionar à playlist">
            {library.playlists.length === 0
              ? <ContextMenuSubmenuItem label="Nenhuma playlist criada" disabled onClick={() => {}} />
              : library.playlists.map(name => (
                <ContextMenuSubmenuItem key={name} label={name} onClick={() => addToPlaylist(name, songContextMenu.file)} />
              ))}
          </ContextMenuSubmenu>
        </div>
      )}

      {addMessage && <div className="playlist-save-msg">{addMessage}</div>}
    </>
  );
}
