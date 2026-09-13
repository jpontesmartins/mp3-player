import { useMemo } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { filterPlaylist } from '../../../shared/lib/search';

export function usePlaylist() {
  const library = useLibrary();

  const filteredFiles = useMemo(
    () => filterPlaylist(library.playlistFiles, '', library.id3Cache),
    [library.playlistFiles, library.id3Cache],
  );

  return {
    files: library.playlistFiles,
    id3Cache: library.id3Cache,
    id3Loading: library.id3Loading,
    id3Loaded: library.id3Loaded,
    id3Total: library.id3Total,
    filteredFiles,
  };
}
