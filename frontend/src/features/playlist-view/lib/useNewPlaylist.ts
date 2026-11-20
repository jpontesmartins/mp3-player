import { useCallback, useState } from 'react';
import * as playlistApi from '../../../shared/api/playlist';
import { isDuplicatePlaylistName } from './playlistName';

interface UseNewPlaylistOptions {
  file: string;
  existing: string[];
  refresh: () => Promise<void>;
  onCreated: (name: string) => void;
  onClose: () => void;
}

export function useNewPlaylist({ file, existing, refresh, onCreated, onClose }: UseNewPlaylistOptions) {
  const [name, setNameState] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const setName = useCallback((value: string) => {
    setNameState(value);
    setError('');
  }, []);

  const canSave = name.trim() !== '' && !saving;

  const submit = useCallback(async () => {
    const trimmed = name.trim();
    if (!trimmed || saving) return;
    setSaving(true);
    setError('');
    const names = (await playlistApi.listPlaylists()) ?? existing;
    if (isDuplicatePlaylistName(trimmed, names)) {
      setError('Já existe uma playlist com esse nome');
      setSaving(false);
      return;
    }
    const ok = await playlistApi.savePlaylist(trimmed, [file]);
    if (!ok) {
      setError('Erro ao salvar playlist');
      setSaving(false);
      return;
    }
    await refresh();
    onCreated(trimmed);
    onClose();
  }, [name, saving, existing, file, refresh, onCreated, onClose]);

  return { name, setName, error, saving, canSave, submit };
}
