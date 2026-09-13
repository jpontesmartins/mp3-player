import { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { useAlbums } from '../lib/useAlbums';
import { useBulkEdit, EDITABLE_FIELDS, FIELD_LABELS, emptyRow, fromTags, isDirty, type EditableField } from '../lib/useBulkEdit';
import { fileName } from '../../../shared/lib/format';
import { revealItemInDir } from '@tauri-apps/plugin-opener';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';
import { CtxMenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';
import PlaylistManager from './PlaylistManager';
import BulkId3Editor from './BulkId3Editor';

interface Selection { type: 'album' | 'artist'; key: string; name: string; files: string[] | null; }
type Edits = Map<string, Record<EditableField, string>>;

function colClass(f: EditableField): string {
  return f === 'year' ? 'col-year' : f === 'track' ? 'col-track' : f === 'disc' ? 'col-disc' : '';
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
    return library.libraryFiles.filter(f => (library.id3Cache.get(f)?.artist?.trim() ?? '') === selected.key);
  }, [selected, library.libraryFiles, library.id3Cache]);

  useEffect(() => {
    const next = new Map<string, Record<EditableField, string>>();
    for (const f of gridFiles) next.set(f, fromTags(library.id3Cache.get(f)));
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
    setEdits(prev => { const next = new Map(prev); const row = next.get(file) ?? emptyRow(); next.set(file, { ...row, [field]: value }); return next; });
  }, []);

  const handleSave = useCallback(async () => {
    const changed: Array<{ file: string; tags: Record<string, string> }> = [];
    for (const [file, row] of edits) {
      if (isDirty(row, library.id3Cache.get(file))) changed.push({ file, tags: row });
    }
    if (changed.length === 0) { setMessage('Nenhuma alteração'); return; }
    setSaving(true);
    setMessage('');
    const { ok, fail } = await updateTags(changed);
    setMessage(fail === 0 ? `${ok} salvo(s)` : `${ok} salvo(s), ${fail} com erro`);
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
                  {EDITABLE_FIELDS.map(f => <th key={f} className={colClass(f)}>{FIELD_LABELS[f]}</th>)}
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
                      {EDITABLE_FIELDS.map(f => (
                        <td key={f} className={colClass(f)}>
                          <input className={`collection-cell-input cell-${f}`} value={row[f]} onChange={e => handleFieldChange(file, f, e.target.value)} />
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
          <CtxMenuItem icon={<FolderOpenIcon />} label="Abrir pasta no explorer" onClick={() => openFolderInExplorer(albumCtxMenu.album.folder)} />
          <CtxMenuItem icon={<ContentCopyIcon />} label="Copiar caminho" onClick={() => copyPath(albumCtxMenu.album.folder)} />
          <div className="ctx-menu-separator" />
          <CtxMenuItem icon={<DriveFileRenameOutlineIcon />} label="Editar ID3 em massa" onClick={() => openBulkForAlbum(albumCtxMenu.album.folder)} />
        </div>
      )}
    </section>
  );
}
