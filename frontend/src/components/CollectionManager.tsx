import { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import type { Id3Tags } from '../App';
import PlaylistManager from './PlaylistManager';
import BulkId3Editor from './BulkId3Editor';
import { revealItemInDir } from '@tauri-apps/plugin-opener';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DriveFileRenameOutlineIcon from '@mui/icons-material/DriveFileRenameOutline';

import { API } from '../config';

const EDITABLE_FIELDS = ['title', 'artist', 'album', 'genre', 'track', 'disc', 'year'] as const;
type EditableField = typeof EDITABLE_FIELDS[number];

const FIELD_LABELS: Record<EditableField, string> = {
  title: 'Música',
  artist: 'Artista',
  album: 'Álbum',
  genre: 'Gênero',
  track: 'Faixa',
  disc: 'Disco',
  year: 'Ano',
};

function colClass(f: EditableField): string {
  return f === 'year' ? 'col-year' : f === 'track' ? 'col-track' : f === 'disc' ? 'col-disc' : '';
}

interface Props {
  libraryFiles: string[];
  id3Cache: Map<string, Id3Tags>;
  onTagsUpdated: (file: string, tags: Id3Tags) => void;
  playlists: string[];
  onRefreshPlaylists: () => Promise<void>;
  onLoadPlaylist: (name: string) => Promise<boolean>;
  onLoadAll: () => void;
}

interface Album {
  folder: string;
  name: string;
  files: string[];
}

interface Selection {
  type: 'album' | 'artist';
  key: string;
  name: string;
  files: string[] | null;
}

type Edits = Map<string, Record<EditableField, string>>;

function parentDir(p: string): string {
  const idx = Math.max(p.lastIndexOf('\\'), p.lastIndexOf('/'));
  return idx < 0 ? p : p.substring(0, idx);
}

function folderName(p: string): string {
  const idx = Math.max(p.lastIndexOf('\\'), p.lastIndexOf('/'));
  return idx < 0 ? p : p.substring(idx + 1);
}

function fileName(p: string): string {
  return p.split('\\').pop()!.split('/').pop()!;
}

function emptyRow(): Record<EditableField, string> {
  return { title: '', artist: '', album: '', genre: '', track: '', disc: '', year: '' };
}

function fromTags(tags: Id3Tags | undefined): Record<EditableField, string> {
  return {
    title: tags?.title ?? '',
    artist: tags?.artist ?? '',
    album: tags?.album ?? '',
    genre: tags?.genre ?? '',
    track: tags?.track ?? '',
    disc: tags?.disc ?? '',
    year: tags?.year ?? '',
  };
}

function isDirty(row: Record<EditableField, string>, tags: Id3Tags | undefined): boolean {
  return EDITABLE_FIELDS.some(k => row[k] !== (tags?.[k] ?? ''));
}

function CtxMenuItem({ icon, label, shortcut, onClick }: { icon: React.ReactNode; label: string; shortcut?: string; onClick: () => void }) {
  return (
    <button type="button" className="ctx-menu-item" onClick={onClick}>
      <span className="ctx-menu-icon">{icon}</span>
      <span className="ctx-menu-label">{label}</span>
      {shortcut && <span className="ctx-menu-shortcut">{shortcut}</span>}
    </button>
  );
}

export default function CollectionManager({ libraryFiles, id3Cache, onTagsUpdated, playlists, onRefreshPlaylists, onLoadPlaylist, onLoadAll }: Props) {
  const [selected, setSelected] = useState<Selection | null>(null);
  const [edits, setEdits] = useState<Edits>(new Map());
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [playlistView, setPlaylistView] = useState(false);
  const [bulkView, setBulkView] = useState(false);
  const [bulkInitialPath, setBulkInitialPath] = useState('');
  const [albumCtxMenu, setAlbumCtxMenu] = useState<{ x: number; y: number; album: Album } | null>(null);
  const albumCtxMenuRef = useRef<HTMLDivElement>(null);

  const albums = useMemo<Album[]>(() => {
    const map = new Map<string, { folder: string; files: string[]; albumNames: string[] }>();
    for (const f of libraryFiles) {
      const folder = parentDir(f);
      const entry = map.get(folder) ?? { folder, files: [], albumNames: [] };
      entry.files.push(f);
      const album = id3Cache.get(f)?.album?.trim();
      if (album) entry.albumNames.push(album);
      map.set(folder, entry);
    }
    return Array.from(map.values())
      .map(entry => {
        const counts = new Map<string, number>();
        for (const name of entry.albumNames) counts.set(name, (counts.get(name) ?? 0) + 1);
        let best: string | undefined;
        let bestCount = 0;
        for (const [name, count] of counts) {
          if (count > bestCount) { best = name; bestCount = count; }
        }
        return { folder: entry.folder, name: best ?? folderName(entry.folder), files: entry.files };
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [libraryFiles, id3Cache]);

  const artists = useMemo<Array<{ name: string; count: number }>>(() => {
    const counts = new Map<string, number>();
    for (const f of libraryFiles) {
      const artist = id3Cache.get(f)?.artist?.trim();
      if (artist) counts.set(artist, (counts.get(artist) ?? 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [libraryFiles, id3Cache]);

  const gridFiles = useMemo<string[]>(() => {
    if (!selected) return [];
    if (selected.type === 'album') return selected.files ?? [];
    return libraryFiles.filter(f => (id3Cache.get(f)?.artist?.trim() ?? '') === selected.key);
  }, [selected, libraryFiles, id3Cache]);

  useEffect(() => {
    const next = new Map<string, Record<EditableField, string>>();
    for (const f of gridFiles) next.set(f, fromTags(id3Cache.get(f)));
    setEdits(next);
    setMessage('');
  }, [selected?.key]);

  useEffect(() => {
    if (!albumCtxMenu) return undefined;
    const close = () => setAlbumCtxMenu(null);
    window.addEventListener('mousedown', close);
    window.addEventListener('scroll', close, true);
    window.addEventListener('resize', close);
    return () => {
      window.removeEventListener('mousedown', close);
      window.removeEventListener('scroll', close, true);
      window.removeEventListener('resize', close);
    };
  }, [albumCtxMenu]); // eslint-disable-line react-hooks/exhaustive-deps

  const selectAlbum = useCallback((album: Album) => {
    setSelected({ type: 'album', key: album.folder, name: album.name, files: album.files });
  }, []);

  const selectArtist = useCallback((name: string) => {
    setSelected({ type: 'artist', key: name, name, files: null });
  }, []);

  const handleAlbumContextMenu = useCallback((e: React.MouseEvent<HTMLLIElement>, album: Album) => {
    e.preventDefault();
    e.stopPropagation();
    let x = e.clientX + 4;
    let y = e.clientY + 4;
    const menu = albumCtxMenuRef.current;
    if (menu) {
      const r = menu.getBoundingClientRect();
      if (x + r.width > window.innerWidth) x = e.clientX - r.width - 4;
      if (y + r.height > window.innerHeight) y = e.clientY - r.height - 4;
    }
    setAlbumCtxMenu({ x: Math.max(4, x), y: Math.max(4, y), album });
  }, []);

  const openBulkForAlbum = useCallback((folder: string) => {
    setAlbumCtxMenu(null);
    setBulkInitialPath(folder);
    setBulkView(true);
  }, []);

  const openFolderInExplorer = useCallback(async (path: string) => {
    setAlbumCtxMenu(null);
    try { await revealItemInDir(path); } catch { /* noop */ }
  }, []);

  const copyPath = useCallback((path: string) => {
    setAlbumCtxMenu(null);
    navigator.clipboard.writeText(path).catch(() => {});
  }, []);

  const handleFieldChange = useCallback((file: string, field: EditableField, value: string) => {
    setEdits(prev => {
      const next = new Map(prev);
      const row = next.get(file) ?? emptyRow();
      next.set(file, { ...row, [field]: value });
      return next;
    });
  }, []);

  const handleSave = useCallback(async () => {
    const changed: Array<{ file: string; tags: Record<EditableField, string> }> = [];
    for (const [file, row] of edits) {
      if (isDirty(row, id3Cache.get(file))) changed.push({ file, tags: row });
    }
    if (changed.length === 0) {
      setMessage('Nenhuma alteração');
      return;
    }
    setSaving(true);
    setMessage('');
    try {
      let ok = 0;
      let fail = 0;
      for (const c of changed) {
        const res = await fetch(`${API}/id3/update`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ path: c.file, tags: c.tags }),
        });
        if (res.ok) {
          const updated: Id3Tags = await res.json();
          onTagsUpdated(c.file, updated);
          ok++;
        } else {
          fail++;
        }
      }
      setMessage(fail === 0 ? `${ok} salvo(s)` : `${ok} salvo(s), ${fail} com erro`);
    } catch {
      setMessage('Erro ao conectar com o servidor');
    } finally {
      setSaving(false);
    }
  }, [edits, id3Cache, onTagsUpdated]);

  return (
    <section id="collection-panel">
      <h2 className="settings-title">Coleção</h2>

      {playlistView ? (
        <>
          <div className="collection-backrow">
            <button className="pmanager-btn" onClick={() => setPlaylistView(false)}>← Voltar</button>
          </div>
          <PlaylistManager
            playlists={playlists}
            collectionFiles={libraryFiles}
            id3Cache={id3Cache}
            onRefreshPlaylists={onRefreshPlaylists}
            onLoadPlaylist={onLoadPlaylist}
          />
        </>
      ) : bulkView ? (
        <>
          <div className="collection-backrow">
            <button className="pmanager-btn" onClick={() => { setBulkView(false); setBulkInitialPath(''); }}>← Voltar</button>
          </div>
          <BulkId3Editor
            collectionFiles={libraryFiles}
            onTagsUpdated={onTagsUpdated}
            initialPath={bulkInitialPath || undefined}
          />
        </>
      ) : (
      <>
      <div className="collection-actions-row">
        <button className="pmanager-btn primary" onClick={() => setBulkView(true)}>
          Edição de ID3 em massa
        </button>
      </div>
      <div className="collection-lists">
        <div className="collection-list">
          <h3 className="collection-list-title">Álbuns ({albums.length})</h3>
          <ul className="collection-items">
            {albums.map(album => (
              <li
                key={album.folder}
                className={selected?.type === 'album' && selected.key === album.folder ? 'active' : ''}
                onClick={() => selectAlbum(album)}
                onContextMenu={e => handleAlbumContextMenu(e, album)}
              >
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
              <li
                key={artist.name}
                className={selected?.type === 'artist' && selected.key === artist.name ? 'active' : ''}
                onClick={() => selectArtist(artist.name)}
              >
                <span className="collection-item-name">{artist.name}</span>
                <span className="collection-item-count">{artist.count}</span>
              </li>
            ))}
            {artists.length === 0 && <li className="collection-empty">Nenhum artista</li>}
          </ul>
        </div>

        <div className="collection-list">
          <h3 className="collection-list-title">Playlists ({playlists.length + 1})</h3>
          <ul className="collection-items">
            <li onClick={onLoadAll}>
              <span className="collection-item-name">Todos os arquivos</span>
              <span className="collection-item-count">{libraryFiles.length}</span>
            </li>
            {playlists.map(name => (
              <li key={name} onClick={() => { setPlaylistView(true); }}>
                <span className="collection-item-name">{name}</span>
              </li>
            ))}
          </ul>
          <button
            className="pmanager-btn primary collection-newplaylist-btn"
            onClick={() => setPlaylistView(true)}
          >
            Gerenciar playlists
          </button>
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
              <button id="collection-save-btn" onClick={handleSave} disabled={saving}>
                {saving ? 'Salvando...' : 'Salvar alterações'}
              </button>
            </div>
          </div>

          <div className="collection-grid-scroll">
            <table className="collection-grid">
              <thead>
                <tr>
                  {EDITABLE_FIELDS.map(f => (
                    <th key={f} className={colClass(f)}>
                      {FIELD_LABELS[f]}
                    </th>
                  ))}
                  <th>Arquivo</th>
                </tr>
              </thead>
              <tbody>
                {gridFiles.map(file => {
                  const row = edits.get(file) ?? emptyRow();
                  const tags = id3Cache.get(file);
                  const dirty = isDirty(row, tags);
                  return (
                    <tr key={file} className={dirty ? 'dirty' : ''}>
                      {EDITABLE_FIELDS.map(f => (
                        <td key={f} className={colClass(f)}>
                          <input
                            className={`collection-cell-input cell-${f}`}
                            value={row[f]}
                            onChange={e => handleFieldChange(file, f, e.target.value)}
                          />
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
        <div
          ref={albumCtxMenuRef}
          id="album-context-menu"
          style={{ left: albumCtxMenu.x, top: albumCtxMenu.y }}
          onMouseDown={e => e.stopPropagation()}
        >
          <CtxMenuItem
            icon={<FolderOpenIcon />}
            label="Abrir pasta no explorer"
            onClick={() => openFolderInExplorer(albumCtxMenu.album.folder)}
          />
          <CtxMenuItem
            icon={<ContentCopyIcon />}
            label="Copiar caminho"
            onClick={() => copyPath(albumCtxMenu.album.folder)}
          />
          <div className="ctx-menu-separator" />
          <CtxMenuItem
            icon={<DriveFileRenameOutlineIcon />}
            label="Editar ID3 em massa"
            onClick={() => openBulkForAlbum(albumCtxMenu.album.folder)}
          />
        </div>
      )}
    </section>
  );
}
