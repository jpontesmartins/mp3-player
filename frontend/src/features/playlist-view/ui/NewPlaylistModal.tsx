import { useEffect, useRef } from 'react';
import { Modal } from '../../../shared/ui/Modal';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { useNewPlaylist } from '../lib/useNewPlaylist';

interface Props {
  file: string;
  onClose: () => void;
  onCreated: (name: string) => void;
}

export default function NewPlaylistModal({ file, onClose, onCreated }: Props) {
  const library = useLibrary();
  const inputRef = useRef<HTMLInputElement>(null);
  const { name, setName, error, saving, canSave, submit } = useNewPlaylist({
    file,
    existing: library.playlists,
    refresh: library.refreshPlaylists,
    onCreated,
    onClose,
  });

  useEffect(() => { inputRef.current?.focus(); }, []);

  return (
    <Modal onClose={onClose} overlayClass="confirm-overlay" dialogClass="confirm-dialog">
      <div role="dialog" aria-modal="true" aria-labelledby="new-playlist-title">
        <p id="new-playlist-title" className="new-playlist-title">Nova playlist</p>
        <input
          ref={inputRef}
          className="new-playlist-input"
          placeholder="Nome da playlist"
          aria-label="Nome da playlist"
          value={name}
          onChange={e => setName(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') submit(); }}
        />
        {error && <div className="new-playlist-error" role="alert">{error}</div>}
        <div className="confirm-actions">
          <button className="pmanager-btn" onClick={onClose}>Cancelar</button>
          <button className="pmanager-btn primary" onClick={submit} disabled={!canSave}>{saving ? '...' : 'Salvar'}</button>
        </div>
      </div>
    </Modal>
  );
}
