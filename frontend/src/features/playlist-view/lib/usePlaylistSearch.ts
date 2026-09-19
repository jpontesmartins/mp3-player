import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { filterPlaylist } from '../../../shared/lib/search';
import * as playlistApi from '../../../shared/api/playlist';

export function usePlaylistSearch() {
  const library = useLibrary();
  const [query, setQuery] = useState('');
  const [searchFocused, setSearchFocused] = useState(false);
  const [saveName, setSaveName] = useState('');
  const [saveOpen, setSaveOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState('');
  const textareaRef = useRef<HTMLInputElement>(null);
  const saveInputRef = useRef<HTMLInputElement>(null);
  const saveMessageTimer = useRef<number | null>(null);

  const filteredFiles = useMemo(
    () => filterPlaylist(library.playlistFiles, query, library.id3Cache),
    [library.playlistFiles, query, library.id3Cache],
  );
  const isFiltered = query.trim().length > 0 && filteredFiles.length < library.playlistFiles.length;
  const searchExpanded = searchFocused || query.trim().length > 0;

  useEffect(() => { if (saveOpen && saveInputRef.current) saveInputRef.current.focus(); }, [saveOpen]);
  useEffect(() => () => { if (saveMessageTimer.current !== null) window.clearTimeout(saveMessageTimer.current); }, []);

  const handleSaveConfirm = useCallback(async () => {
    const name = saveName.trim();
    if (!name || filteredFiles.length === 0) return;
    setSaving(true);
    const success = await playlistApi.savePlaylist(name, filteredFiles);
    if (success) {
      setSaveMessage(`Playlist "${name}" salva (${filteredFiles.length} músicas)`);
      setSaveOpen(false);
      setSaveName('');
      await library.refreshPlaylists();
    } else {
      setSaveMessage('Erro ao salvar playlist');
    }
    setSaving(false);
    if (saveMessageTimer.current !== null) window.clearTimeout(saveMessageTimer.current);
    saveMessageTimer.current = window.setTimeout(() => setSaveMessage(''), 4000);
  }, [saveName, filteredFiles, library]);

  return {
    query, setQuery,
    searchFocused, setSearchFocused,
    filteredFiles, isFiltered, searchExpanded,
    saveName, setSaveName,
    saveOpen, setSaveOpen,
    saving, saveMessage,
    textareaRef, saveInputRef,
    handleSaveConfirm,
  };
}
