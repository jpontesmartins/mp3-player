import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';
import type { Id3Tags } from '../../shared/types';
import * as id3Api from '../../shared/api/id3';
import * as playlistApi from '../../shared/api/playlist';

const ID3_BATCH_SIZE = 50;

interface LibraryState {
  libraryFiles: string[];
  playlists: string[];
  id3Cache: Map<string, Id3Tags>;
  id3Loading: boolean;
  id3Loaded: number;
  id3Total: number;
  playlistFiles: string[];
}

interface LibraryActions {
  loadFolder: (path: string, forceRefresh?: boolean) => Promise<boolean>;
  loadVirtualPlaylist: (name: string) => Promise<boolean>;
  refreshPlaylists: () => Promise<void>;
  updateId3Cache: (file: string, tags: Id3Tags) => void;
  setPlaylistFiles: (files: string[]) => void;
  setLibraryFiles: (files: string[]) => void;
  resetPlaybackState: () => void;
}

const LibraryContext = createContext<LibraryState & LibraryActions>(null!);

const STORAGE_KEY = 'mp3_folder';

export function LibraryProvider({ children }: { children: ReactNode }) {
  const [libraryFiles, setLibraryFiles] = useState<string[]>([]);
  const [playlistFiles, setPlaylistFiles] = useState<string[]>([]);
  const [playlists, setPlaylists] = useState<string[]>([]);
  const [id3Cache, setId3Cache] = useState<Map<string, Id3Tags>>(new Map());
  const [id3Loading, setId3Loading] = useState(false);
  const [id3Loaded, setId3Loaded] = useState(0);
  const [id3Total, setId3Total] = useState(0);

  const resetPlaybackState = useCallback(() => {}, []);

  const refreshPlaylists = useCallback(async () => {
    const data = await playlistApi.listPlaylists();
    if (data) setPlaylists(data);
  }, []);

  const loadFolder = useCallback(async (folder: string, forceRefresh = false): Promise<boolean> => {
    const files = await playlistApi.loadFolder(folder);
    if (!files) return false;
    setPlaylistFiles(files);
    setLibraryFiles(files);
    localStorage.setItem(STORAGE_KEY, folder);
    if (files.length > 0) {
      setId3Loading(true);
      setId3Total(files.length);
      setId3Loaded(0);
      try {
        for (let i = 0; i < files.length; i += ID3_BATCH_SIZE) {
          const batch = files.slice(i, i + ID3_BATCH_SIZE);
          const tagsMap = await id3Api.bulkId3(batch, forceRefresh);
          if (tagsMap) {
            setId3Cache(prev => {
              const next = new Map(prev);
              for (const [k, v] of Object.entries(tagsMap)) next.set(k, v);
              return next;
            });
            setId3Loaded(i + batch.length);
          }
        }
      } finally {
        setId3Loading(false);
        setTimeout(() => setId3Total(0), 1000);
      }
    }
    return true;
  }, []);

  const loadVirtualPlaylist = useCallback(async (name: string): Promise<boolean> => {
    const files = await playlistApi.loadVirtual(name);
    if (!files) return false;
    setPlaylistFiles(files);
    if (files.length > 0) {
      setId3Loading(true);
      setId3Total(files.length);
      try {
        const allFiles = [...new Set([...libraryFiles, ...files])];
        const missing = allFiles.filter(f => !id3Cache.has(f));
        if (missing.length > 0) {
          for (let i = 0; i < missing.length; i += ID3_BATCH_SIZE) {
            const batch = missing.slice(i, i + ID3_BATCH_SIZE);
            const tagsMap = await id3Api.bulkId3(batch);
            if (tagsMap) {
              setId3Cache(prev => {
                const next = new Map(prev);
                for (const [k, v] of Object.entries(tagsMap)) next.set(k, v);
                return next;
              });
              setId3Loaded(prev => prev + batch.length);
            }
          }
        }
      } finally {
        setId3Loading(false);
      }
    }
    return true;
  }, [libraryFiles, id3Cache]);

  const updateId3Cache = useCallback((file: string, tags: Id3Tags) => {
    setId3Cache(prev => {
      const next = new Map(prev);
      next.set(file, tags);
      return next;
    });
  }, []);

  return (
    <LibraryContext.Provider value={{
      libraryFiles, playlists, id3Cache, id3Loading, id3Loaded, id3Total, playlistFiles,
      loadFolder, loadVirtualPlaylist, refreshPlaylists, updateId3Cache,
      setPlaylistFiles, setLibraryFiles, resetPlaybackState,
    }}>
      {children}
    </LibraryContext.Provider>
  );
}

export function useLibrary() {
  return useContext(LibraryContext);
}
