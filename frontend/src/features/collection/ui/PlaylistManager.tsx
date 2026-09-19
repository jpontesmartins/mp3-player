import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import SearchIcon from '@mui/icons-material/Search';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { displayName } from '../../../shared/lib/format';
import { filterPlaylist } from '../../../shared/lib/search';
import * as playlistApi from '../../../shared/api/playlist';

export default function PlaylistManager() {
  const library = useLibrary();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [right, setRight] = useState<string[]>([]);
  const [newName, setNewName] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [listMessage, setListMessage] = useState('');
  const [query, setQuery] = useState('');
  const [dragFile, setDragFile] = useState<string | null>(null);
  const [dragPos, setDragPos] = useState<{ x: number; y: number } | null>(null);
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null);
  const dragOffset = useRef({ x: 0, y: 0 });
  const rightPaneRef = useRef<HTMLDivElement>(null);
  const dragFileRef = useRef<string | null>(null);

  const left = library.libraryFiles;
  const filteredLeft = useMemo(() => filterPlaylist(left, query, library.id3Cache), [left, query, library.id3Cache]);
  const rightSet = useMemo(() => new Set(right), [right]);

  useEffect(() => {
    if (!dragFile) return undefined;
    document.body.classList.add('is-dragging');
    dragFileRef.current = dragFile;
    const onMove = (e: MouseEvent) => setDragPos({ x: e.clientX - dragOffset.current.x, y: e.clientY - dragOffset.current.y });
    const onUp = (e: MouseEvent) => {
      setDragFile(null); setDragPos(null); document.body.classList.remove('is-dragging');
      const pane = rightPaneRef.current;
      if (pane) {
        const rect = pane.getBoundingClientRect();
        if (e.clientX >= rect.left && e.clientX <= rect.right && e.clientY >= rect.top && e.clientY <= rect.bottom) {
          const file = dragFileRef.current;
          if (file) setRight(previous => (previous.includes(file) ? previous : [...previous, file]));
        }
      }
      dragFileRef.current = null;
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    return () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp); document.body.classList.remove('is-dragging'); };
  }, [dragFile]);

  const startNew = () => { setName(newName.trim()); setRight([]); setError(''); setMessage(''); setQuery(''); setEditing(true); };

  const openPlaylist = async (playlist: string) => {
    const paths = await playlistApi.loadVirtual(playlist);
    if (paths) { setName(playlist); setRight(paths); setError(''); setMessage(''); setEditing(true); }
    else setListMessage('Erro ao carregar playlist');
  };

  const close = () => { setEditing(false); setName(''); setRight([]); setError(''); setMessage(''); };

  const handleSongMouseDown = useCallback((e: React.MouseEvent, file: string) => {
    if (e.button !== 0) return;
    const target = e.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    dragOffset.current = { x: e.clientX - rect.left, y: e.clientY - rect.top };
    setDragFile(file);
    setDragPos({ x: e.clientX - dragOffset.current.x, y: e.clientY - dragOffset.current.y });
  }, []);

  const removePlaylist = async (playlist: string) => {
    const success = await playlistApi.deletePlaylist(playlist);
    if (success) { setListMessage(`Playlist "${playlist}" excluída`); await library.refreshPlaylists(); }
    else setListMessage('Erro ao excluir');
  };

  const save = async () => {
    const trimmed = name.trim();
    if (!trimmed) { setError('Informe um nome para a playlist'); return; }
    if (right.length === 0) { setError('Adicione pelo menos uma música'); return; }
    setSaving(true); setError(''); setMessage('');
    const success = await playlistApi.savePlaylist(trimmed, right);
    if (success) { setName(trimmed); setMessage(`Playlist "${trimmed}" salva (${right.length} músicas)`); await library.refreshPlaylists(); }
    else setError('Erro ao salvar');
    setSaving(false);
  };

  return (
    <div className="pmanager">
      {!editing ? (
        <div className="pmanager-list">
          <div className="pmanager-newrow">
            <input className="pmanager-newinput" placeholder="Nome da nova playlist" value={newName} onChange={e => setNewName(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') startNew(); }} />
            <button className="pmanager-btn primary" onClick={startNew}>Nova playlist</button>
          </div>
          {listMessage && <div className="pmanager-msg">{listMessage}</div>}
          {library.playlists.length === 0 ? <div className="collection-empty">Nenhuma playlist salva</div> : (
            <ul className="collection-items">
              {library.playlists.map(playlist => (
                <li key={playlist} className="pmanager-item">
                  <span className="collection-item-name" onClick={() => openPlaylist(playlist)}>{playlist}</span>
                  <div className="pmanager-item-actions">
                    <button className="pmanager-btn" onClick={() => { library.loadVirtualPlaylist(playlist); }}>Carregar</button>
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
            <input className="pmanager-nameinput" placeholder="Nome da playlist" value={name} onChange={e => setName(e.target.value)} />
            <div className="pmanager-editor-actions">
              <button className="pmanager-btn" onClick={() => library.loadVirtualPlaylist(name.trim())} disabled={!name.trim()}>Carregar</button>
              <button className="pmanager-btn primary" onClick={save} disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</button>
            </div>
          </div>
          {message && <div className="pmanager-msg">{message}</div>}
          {error && <div className="pmanager-error">{error}</div>}
          <div className="pmanager-panes">
            <div className="pmanager-pane">
              <h4 className="pmanager-pane-title">Todas as músicas ({filteredLeft.length})</h4>
              <div className="pmanager-search-bar">
                <span className="search-icon"><SearchIcon /></span>
                <input className="pmanager-search-input" placeholder='<genre> == rock && <year> > 2000' type="text" value={query} onChange={e => setQuery(e.target.value)} onKeyDown={e => { if (e.key === 'Escape') setQuery(''); }} />
                {query.trim() && <span className="pmanager-search-count">{filteredLeft.length} / {left.length}</span>}
              </div>
              <ul className="pmanager-songs">
                {filteredLeft.map(file => {
                  const inRight = rightSet.has(file);
                  return (
                    <li key={file} className={`pmanager-song${!inRight ? ' draggable' : ''}`} onMouseDown={!inRight ? e => handleSongMouseDown(e, file) : undefined}>
                      <span className="collection-item-name">{displayName(library.id3Cache.get(file), file)}</span>
                      <button className="pmanager-btn" disabled={inRight} title={inRight ? 'Já adicionada' : 'Adicionar à playlist'} onClick={() => setRight(previous => (previous.includes(file) ? previous : [...previous, file]))}>
                        {inRight ? 'Na lista' : 'Adicionar'}
                      </button>
                    </li>
                  );
                })}
                {filteredLeft.length === 0 && query.trim() && <li className="collection-empty">Nenhum resultado</li>}
              </ul>
            </div>
            <div ref={rightPaneRef} className={`pmanager-pane${dragFile ? ' pmanager-pane-drop' : ''}`}>
              <h4 className="pmanager-pane-title">Na playlist ({right.length})</h4>
              <ul className="pmanager-songs">
                {right.map(file => (
                  <li key={file} className="pmanager-song">
                    <span className="collection-item-name">{displayName(library.id3Cache.get(file), file)}</span>
                    <button className="pmanager-btn danger" onClick={() => setRight(previous => previous.filter(item => item !== file))}>Remover</button>
                  </li>
                ))}
                {right.length === 0 && <li className="collection-empty">{dragFile ? 'Solte aqui para adicionar' : 'Arraste músicas ou clique em "Adicionar"'}</li>}
              </ul>
            </div>
          </div>
        </div>
      )}

      {dragFile && dragPos && (
        <div className="pmanager-drag-ghost" style={{ left: dragPos.x + 12, top: dragPos.y + 8 }}>
          {displayName(library.id3Cache.get(dragFile), dragFile)}
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
