import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import SearchIcon from '@mui/icons-material/Search';
import type { Id3Tags } from '../App';
import { filterPlaylist } from '../searchParser';

import { API } from '../config';

interface Props {
  playlists: string[];
  collectionFiles: string[];
  id3Cache: Map<string, Id3Tags>;
  onRefreshPlaylists: () => Promise<void>;
  onLoadPlaylist: (name: string) => Promise<boolean>;
}

function fileName(path: string): string {
  return path.split('\\').pop()!.split('/').pop()!;
}

function displayName(tags: Id3Tags | undefined, file: string): string {
  if (tags) {
    const artist = tags.artist;
    const title = tags.title;
    if (artist && title) return `${artist} - ${title}`;
    if (title) return title;
  }
  return fileName(file);
}

export default function PlaylistManager({
  playlists,
  collectionFiles,
  id3Cache,
  onRefreshPlaylists,
  onLoadPlaylist,
}: Props) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [right, setRight] = useState<string[]>([]);
  const [newName, setNewName] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [listMsg, setListMsg] = useState('');
  const [query, setQuery] = useState('');
  const [dragFile, setDragFile] = useState<string | null>(null);
  const [dragPos, setDragPos] = useState<{ x: number; y: number } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const dragOffset = useRef({ x: 0, y: 0 });
  const rightPaneRef = useRef<HTMLDivElement>(null);
  const dragFileRef = useRef<string | null>(null);

  const left = collectionFiles;

  const filteredLeft = useMemo(
    () => filterPlaylist(left, query, id3Cache),
    [left, query, id3Cache],
  );

  const rightSet = useMemo(() => new Set(right), [right]);

  useEffect(() => {
    if (!dragFile) return undefined;
    document.body.classList.add('is-dragging');
    dragFileRef.current = dragFile;
    const onMove = (e: MouseEvent) => {
      setDragPos({ x: e.clientX - dragOffset.current.x, y: e.clientY - dragOffset.current.y });
    };
    const onUp = (e: MouseEvent) => {
      setDragFile(null);
      setDragPos(null);
      document.body.classList.remove('is-dragging');
      const pane = rightPaneRef.current;
      if (pane) {
        const r = pane.getBoundingClientRect();
        if (e.clientX >= r.left && e.clientX <= r.right && e.clientY >= r.top && e.clientY <= r.bottom) {
          const file = dragFileRef.current;
          if (file) addToRight(file);
        }
      }
      dragFileRef.current = null;
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      document.body.classList.remove('is-dragging');
    };
  }, [dragFile]);

  const startNew = () => {
    setName(newName.trim());
    setRight([]);
    setError('');
    setMessage('');
    setQuery('');
    setEditing(true);
  };

  const openPlaylist = async (playlist: string) => {
    try {
      const res = await fetch(`${API}/playlist/${encodeURIComponent(playlist)}`);
      if (res.ok) {
        const paths: string[] = await res.json();
        setName(playlist);
        setRight(paths);
        setError('');
        setMessage('');
        setEditing(true);
      } else {
        setListMsg('Erro ao carregar playlist');
      }
    } catch (_) {
      setListMsg('Erro de conexão');
    }
  };

  const close = () => {
    setEditing(false);
    setName('');
    setRight([]);
    setError('');
    setMessage('');
  };

  const addToRight = useCallback((file: string) => {
    setRight(prev => (prev.includes(file) ? prev : [...prev, file]));
  }, []);

  const removeFromRight = (file: string) => {
    setRight(prev => prev.filter(f => f !== file));
  };

  const handleSongMouseDown = useCallback((e: React.MouseEvent, file: string) => {
    if (e.button !== 0) return;
    const target = e.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    dragOffset.current = { x: e.clientX - rect.left, y: e.clientY - rect.top };
    setDragFile(file);
    setDragPos({ x: e.clientX - dragOffset.current.x, y: e.clientY - dragOffset.current.y });
  }, []);

  const removePlaylist = async (playlist: string) => {
    try {
      const res = await fetch(`${API}/playlist/${encodeURIComponent(playlist)}`, { method: 'DELETE' });
      if (res.ok) {
        setListMsg(`Playlist "${playlist}" excluída`);
        await onRefreshPlaylists();
      } else {
        setListMsg('Erro ao excluir');
      }
    } catch (_) {
      setListMsg('Erro de conexão');
    }
  };

  const save = async () => {
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Informe um nome para a playlist');
      return;
    }
    if (right.length === 0) {
      setError('Adicione pelo menos uma música');
      return;
    }
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const res = await fetch(`${API}/playlist`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: trimmed, paths: right }),
      });
      if (res.ok) {
        setName(trimmed);
        setMessage(`Playlist "${trimmed}" salva (${right.length} músicas)`);
        await onRefreshPlaylists();
      } else {
        setError('Erro ao salvar');
      }
    } catch (_) {
      setError('Erro de conexão');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="pmanager">
      {!editing ? (
        <div className="pmanager-list">
          <div className="pmanager-newrow">
            <input
              className="pmanager-newinput"
              placeholder="Nome da nova playlist"
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') startNew(); }}
            />
            <button className="pmanager-btn primary" onClick={startNew}>Nova playlist</button>
          </div>

          {listMsg && <div className="pmanager-msg">{listMsg}</div>}

          {playlists.length === 0
            ? <div className="collection-empty">Nenhuma playlist salva</div>
            : (
              <ul className="collection-items">
                {playlists.map(playlist => (
                  <li key={playlist} className="pmanager-item">
                    <span className="collection-item-name" onClick={() => openPlaylist(playlist)}>
                      {playlist}
                    </span>
                    <div className="pmanager-item-actions">
                      <button className="pmanager-btn" onClick={() => onLoadPlaylist(playlist)}>Carregar</button>
                      <button className="pmanager-btn" onClick={() => openPlaylist(playlist)}>Editar</button>
                      <button className="pmanager-btn danger" onClick={() => setConfirmDelete(playlist)}>Excluir</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
        </div>
      ) : (
        <div className="pmanager-editor">
          <h3 className="pmanager-editor-title">Edição de Playlist</h3>
          <div className="pmanager-editor-header">
            <button className="pmanager-btn" onClick={close}>← Voltar</button>
            <input
              className="pmanager-nameinput"
              placeholder="Nome da playlist"
              value={name}
              onChange={e => setName(e.target.value)}
            />
            <div className="pmanager-editor-actions">
              <button
                className="pmanager-btn"
                onClick={() => onLoadPlaylist(name.trim())}
                disabled={!name.trim()}
              >
                Carregar
              </button>
              <button className="pmanager-btn primary" onClick={save} disabled={saving}>
                {saving ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </div>
          {message && <div className="pmanager-msg">{message}</div>}
          {error && <div className="pmanager-error">{error}</div>}

          <div className="pmanager-panes">
            <div className="pmanager-pane">
              <h4 className="pmanager-pane-title">Todas as músicas ({filteredLeft.length})</h4>
              <div className="pmanager-search-bar">
                <span className="search-icon"><SearchIcon /></span>
                <input
                  className="pmanager-search-input"
                  placeholder='<genre> == rock && <year> > 2000'
                  type="text"
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Escape') setQuery(''); }}
                />
                {query.trim() && (
                  <span className="pmanager-search-count">{filteredLeft.length} / {left.length}</span>
                )}
              </div>
              <ul className="pmanager-songs">
                {filteredLeft.map(file => {
                  const inRight = rightSet.has(file);
                  return (
                    <li
                      key={file}
                      className={`pmanager-song${!inRight ? ' draggable' : ''}`}
                      onMouseDown={!inRight ? e => handleSongMouseDown(e, file) : undefined}
                    >
                      <span className="collection-item-name">{displayName(id3Cache.get(file), file)}</span>
                      <button
                        className="pmanager-btn"
                        disabled={inRight}
                        title={inRight ? 'Já adicionada' : 'Adicionar à playlist'}
                        onClick={() => addToRight(file)}
                      >
                        {inRight ? 'Na lista' : 'Adicionar'}
                      </button>
                    </li>
                  );
                })}
                {filteredLeft.length === 0 && query.trim() && (
                  <li className="collection-empty">Nenhum resultado</li>
                )}
              </ul>
            </div>

            <div
              ref={rightPaneRef}
              className={`pmanager-pane${dragFile ? ' pmanager-pane-drop' : ''}`}
            >
              <h4 className="pmanager-pane-title">Na playlist ({right.length})</h4>
              <ul className="pmanager-songs">
                {right.map(file => (
                  <li key={file} className="pmanager-song">
                    <span className="collection-item-name">{displayName(id3Cache.get(file), file)}</span>
                    <button className="pmanager-btn danger" onClick={() => removeFromRight(file)}>Remover</button>
                  </li>
                ))}
                {right.length === 0 && <li className="collection-empty">{dragFile ? 'Solte aqui para adicionar' : 'Arraste músicas ou clique em "Adicionar"'}</li>}
              </ul>
            </div>
          </div>
        </div>
      )}

      {dragFile && dragPos && (
        <div
          className="pmanager-drag-ghost"
          style={{ left: dragPos.x + 12, top: dragPos.y + 8 }}
        >
          {displayName(id3Cache.get(dragFile), dragFile)}
        </div>
      )}

      {confirmDelete && (
        <div className="confirm-overlay" onClick={() => setConfirmDelete(null)}>
          <div className="confirm-dialog" onMouseDown={e => e.stopPropagation()}>
            <p className="confirm-text">Excluir playlist <strong>{confirmDelete}</strong>?</p>
            <div className="confirm-actions">
              <button className="pmanager-btn" onClick={() => setConfirmDelete(null)}>Cancelar</button>
              <button className="pmanager-btn danger" onClick={() => { removePlaylist(confirmDelete); setConfirmDelete(null); }}>Excluir</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
