import { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { useAlbums } from '../lib/useAlbums';
import { useBulkEdit, EDITABLE_FIELDS, FIELD_LABELS, emptyRow, fromTags, isDirty, type EditableField } from '../lib/useBulkEdit';
import { fileName } from '../../../shared/lib/format';
import { revealItemInDir } from '@tauri-apps/plugin-opener';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';
import { ContextMenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';
import PlaylistManager from './PlaylistManager';
import BulkId3Editor from './BulkId3Editor';

interface Selection { type: 'album' | 'artist'; key: string; name: string; files: string[] | null; }
type Edits = Map<string, Record<EditableField, string>>;

function colClass(field: EditableField): string {
  return field === 'year' ? 'col-year' : field === 'track' ? 'col-track' : field === 'disc' ? 'col-disc' : '';
}

export default function CollectionManager() {
  const library = useLibrary();
  const { albums, artists } = useAlbums();
  const { updateTags } = useBulkEdit();
  const [selected, setSelected] = useState<Selection | null>(null);
  const [edits, setEdits] = useState<Edits>(new Map());
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [playlistView, setPlaylistView] = useState(false);
  const [bulkView, setBulkView] = useState(false);
  const [bulkInitialPath, setBulkInitialPath] = useState('');
  const [albumCtxMenu, setAlbumCtxMenu] = useState<{ x: number; y: number; album: { folder: string; name: string; files: string[] } } | null>(null);
  const albumCtxMenuRef = useRef<HTMLDivElement>(null);

  const gridFiles = useMemo<string[]>(() => {
    if (!selected) return [];
    if (selected.type === 'album') return selected.files ?? [];
    return library.libraryFiles.filter(file => (library.id3Cache.get(file)?.artist?.trim() ?? '') === selected.key);
  }, [selected, library.libraryFiles, library.id3Cache]);

  useEffect(() => {
    const next = new Map<string, Record<EditableField, string>>();
    for (const file of gridFiles) next.set(file, fromTags(library.id3Cache.get(file)));
    setEdits(next);
    setMessage('');
  }, [selected?.key]); // eslint-disable-line react-hooks/exhaustive-deps

  useContextMenuClose(!!albumCtxMenu, () => setAlbumCtxMenu(null));

  const selectAlbum = useCallback((album: { folder: string; name: string; files: string[] }) => {
    setSelected({ type: 'album', key: album.folder, name: album.name, files: album.files });
  }, []);

  const selectArtist = useCallback((name: string) => {
    setSelected({ type: 'artist', key: name, name, files: null });
  }, []);

  const handleAlbumContextMenu = useCallback((e: React.MouseEvent<HTMLLIElement>, album: { folder: string; name: string; files: string[] }) => {
    e.preventDefault();
    e.stopPropagation();
    setAlbumCtxMenu({ ...getContextMenuPosition(e, albumCtxMenuRef.current), album });
  }, []);

  const openBulkForAlbum = useCallback((folder: string) => { setAlbumCtxMenu(null); setBulkInitialPath(folder); setBulkView(true); }, []);
  const openFolderInExplorer = useCallback(async (path: string) => { setAlbumCtxMenu(null); try { await revealItemInDir(path); } catch { /* noop */ } }, []);
  const copyPath = useCallback((path: string) => { setAlbumCtxMenu(null); navigator.clipboard.writeText(path).catch(() => {}); }, []);

  const handleFieldChange = useCallback((file: string, field: EditableField, value: string) => {
    setEdits(previous => { const next = new Map(previous); const row = next.get(file) ?? emptyRow(); next.set(file, { ...row, [field]: value }); return next; });
  }, []);

  const handleSave = useCallback(async () => {
    const changed: Array<{ file: string; tags: Record<string, string> }> = [];
    for (const [file, row] of edits) {
      if (isDirty(row, library.id3Cache.get(file))) changed.push({ file, tags: row });
    }
    if (changed.length === 0) { setMessage('Nenhuma alteração'); return; }
    setSaving(true);
    setMessage('');
    const { successCount, failureCount } = await updateTags(changed);
    setMessage(failureCount === 0 ? `${successCount} salvo(s)` : `${successCount} salvo(s), ${failureCount} com erro`);
    setSaving(false);
  }, [edits, library.id3Cache, updateTags]);

  const handleLoadAll = useCallback(() => {
    library.setPlaylistFiles(library.libraryFiles);
  }, [library]);

  return (
    <section id="collection-panel">
      <h2 className="settings-title">Coleção</h2>

      {playlistView ? (
        <>
          <div className="collection-backrow"><button className="pmanager-btn" onClick={() => setPlaylistView(false)}>← Voltar</button></div>
          <PlaylistManager />
        </>
      ) : bulkView ? (
        <>
          <div className="collection-backrow"><button className="pmanager-btn" onClick={() => { setBulkView(false); setBulkInitialPath(''); }}>← Voltar</button></div>
          <BulkId3Editor initialPath={bulkInitialPath || undefined} />
        </>
      ) : (
      <>
      <div className="collection-actions-row">
        <button className="pmanager-btn primary" onClick={() => setBulkView(true)}>Edição de ID3 em massa</button>
      </div>
      <div className="collection-lists">
        <div className="collection-list">
          <h3 className="collection-list-title">Álbuns ({albums.length})</h3>
          <ul className="collection-items">
            {albums.map(album => (
              <li key={album.folder} className={selected?.type === 'album' && selected.key === album.folder ? 'active' : ''} onClick={() => selectAlbum(album)} onContextMenu={e => handleAlbumContextMenu(e, album)}>
                <span className="collection-item-name">{album.name}</span>
                <span className="collection-item-count">{album.files.length}</span>
              </li>
            ))}
            {albums.length === 0 && <li className="collection-empty">Nenhum álbum</li>}
          </ul>
        </div>
        <div className="collection-list">
          <h3 className="collection-list-title">Artistas ({artists.length})</h3>
          <ul className="collection-items">
            {artists.map(artist => (
              <li key={artist.name} className={selected?.type === 'artist' && selected.key === artist.name ? 'active' : ''} onClick={() => selectArtist(artist.name)}>
                <span className="collection-item-name">{artist.name}</span>
                <span className="collection-item-count">{artist.count}</span>
              </li>
            ))}
            {artists.length === 0 && <li className="collection-empty">Nenhum artista</li>}
          </ul>
        </div>
        <div className="collection-list">
          <h3 className="collection-list-title">Playlists ({library.playlists.length + 1})</h3>
          <ul className="collection-items">
            <li onClick={handleLoadAll}><span className="collection-item-name">Todos os arquivos</span><span className="collection-item-count">{library.libraryFiles.length}</span></li>
            {library.playlists.map(name => (
              <li key={name} onClick={() => setPlaylistView(true)}><span className="collection-item-name">{name}</span></li>
            ))}
          </ul>
          <button className="pmanager-btn primary collection-newplaylist-btn" onClick={() => setPlaylistView(true)}>Gerenciar playlists</button>
        </div>
      </div>

      {selected && (
        <div className="collection-grid-wrap">
          <div className="collection-grid-header">
            <h3 className="collection-grid-title">
              {selected.type === 'album' ? `Álbum: ${selected.name}` : `Artista: ${selected.name}`}
              {' '}({gridFiles.length} {gridFiles.length === 1 ? 'música' : 'músicas'})
            </h3>
            <div className="collection-grid-actions">
              {message && <span className="collection-message">{message}</span>}
              <button id="collection-save-btn" onClick={handleSave} disabled={saving}>{saving ? 'Salvando...' : 'Salvar alterações'}</button>
            </div>
          </div>
          <div className="collection-grid-scroll">
            <table className="collection-grid">
              <thead>
                <tr>
                  {EDITABLE_FIELDS.map(field => <th key={field} className={colClass(field)}>{FIELD_LABELS[field]}</th>)}
                  <th>Arquivo</th>
                </tr>
              </thead>
              <tbody>
                {gridFiles.map(file => {
                  const row = edits.get(file) ?? emptyRow();
                  const tags = library.id3Cache.get(file);
                  const dirty = isDirty(row, tags);
                  return (
                    <tr key={file} className={dirty ? 'dirty' : ''}>
                      {EDITABLE_FIELDS.map(field => (
                        <td key={field} className={colClass(field)}>
                          <input className={`collection-cell-input cell-${field}`} value={row[field]} onChange={e => handleFieldChange(file, field, e.target.value)} />
                        </td>
                      ))}
                      <td className="collection-file">{fileName(file)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
      </>
      )}

      {albumCtxMenu && (
        <div ref={albumCtxMenuRef} id="album-context-menu" style={{ left: albumCtxMenu.x, top: albumCtxMenu.y }} onMouseDown={e => e.stopPropagation()}>
          <ContextMenuItem icon={<FolderOpenIcon />} label="Abrir pasta no explorer" onClick={() => openFolderInExplorer(albumCtxMenu.album.folder)} />
          <ContextMenuItem icon={<ContentCopyIcon />} label="Copiar caminho" onClick={() => copyPath(albumCtxMenu.album.folder)} />
          <div className="ctx-menu-separator" />
          <ContextMenuItem icon={<DriveFileRenameOutlineIcon />} label="Editar ID3 em massa" onClick={() => openBulkForAlbum(albumCtxMenu.album.folder)} />
        </div>
      )}
    </section>
  );
}
