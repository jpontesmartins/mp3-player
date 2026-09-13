import { useCallback } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';

const STORAGE_KEY = 'mp3_folder';

export function useLoadFolder() {
  const library = useLibrary();

  const loadFolderWithRetry = useCallback(async (folder: string, forceRefresh = false): Promise<boolean> => {
    library.setPlaylistFiles([]);
    library.setLibraryFiles([]);
    const ok = await library.loadFolder(folder, forceRefresh);
    return ok;
  }, [library]);

  const restoreLastFolder = useCallback(async (): Promise<boolean> => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return false;
    return loadFolderWithRetry(saved);
  }, [loadFolderWithRetry]);

  return { loadFolderWithRetry, restoreLastFolder };
}
