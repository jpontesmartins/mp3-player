import { useState } from 'react';
import type { Id3Tags } from '../App';

const API = 'http://localhost:8111';

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

  const left = collectionFiles;

  const startNew = () => {
    setName(newName.trim());
    setRight([]);
    setError('');
    setMessage('');
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

  const addToRight = (file: string) => {
    setRight(prev => (prev.includes(file) ? prev : [...prev, file]));
  };

  const removeFromRight = (file: string) => {
    setRight(prev => prev.filter(f => f !== file));
  };

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
                      <button className="pmanager-btn danger" onClick={() => removePlaylist(playlist)}>Excluir</button>
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
              <h4 className="pmanager-pane-title">Todas as músicas ({left.length})</h4>
              <ul className="pmanager-songs">
                {left.map(file => {
                  const inRight = right.includes(file);
                  return (
                    <li key={file} className="pmanager-song">
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
              </ul>
            </div>

            <div className="pmanager-pane">
              <h4 className="pmanager-pane-title">Na playlist ({right.length})</h4>
              <ul className="pmanager-songs">
                {right.map(file => (
                  <li key={file} className="pmanager-song">
                    <span className="collection-item-name">{displayName(id3Cache.get(file), file)}</span>
                    <button className="pmanager-btn danger" onClick={() => removeFromRight(file)}>Remover</button>
                  </li>
                ))}
                {right.length === 0 && <li className="collection-empty">Nenhuma música adicionada</li>}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}