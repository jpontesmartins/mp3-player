import { useState, useEffect, useCallback, useRef } from 'react';
import Toolbar from './components/Toolbar';
import Player from './components/Player';
import Playlist from './components/Playlist';
import LyricsPanel from './components/LyricsPanel';
import SettingsPanel from './components/SettingsPanel';
import CollectionManager from './components/CollectionManager';
import InfoModal from './components/InfoModal';
import type { PlaybackMode } from './components/SettingsPanel';
import './App.css';

import { API } from './config';
const STORAGE_KEY = 'mp3_folder';
const THEME_KEY = 'mp3_theme';

export type AppTheme = 'dark' | 'light';

function loadTheme(): AppTheme {
  return localStorage.getItem(THEME_KEY) === 'light' ? 'light' : 'dark';
}

export interface Id3Tags {
  title?: string;
  artist?: string;
  album?: string;
  year?: string;
  genre?: string;
  track?: string;
  disc?: string;
  duration_ms?: string;
  kbps?: string;
  error?: string;
}



interface PlayingData {
  status: 'playing' | 'paused' | 'stopped';
  file: string;
  position: number;
  duration: number;
  id3?: Id3Tags;
}

type Status = 'playing' | 'paused' | 'stopped';

function getNextFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[0];
  const idx = files.indexOf(current);
  if (idx < 0 || idx >= files.length - 1) return files[0];
  return files[idx + 1];
}

function getPrevFile(current: string | null, files: string[], mode: PlaybackMode): string | null {
  if (!files.length) return null;
  if (mode === 'repeat') return current;
  if (mode === 'shuffle') return files[Math.floor(Math.random() * files.length)];
  if (!current) return files[files.length - 1];
  const idx = files.indexOf(current);
  if (idx <= 0) return files[files.length - 1];
  return files[idx - 1];
}

export default function App() {
  const [playlistFiles, setPlaylistFiles] = useState<string[]>([]);
  const [libraryFiles, setLibraryFiles] = useState<string[]>([]);
  const [playlists, setPlaylists] = useState<string[]>([]);
  const [currentFile, setCurrentFile] = useState<string | null>(null);
  const [status, setStatus] = useState<Status>('stopped');
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [id3Cache, setId3Cache] = useState<Map<string, Id3Tags>>(new Map());
  const [id3Loading, setId3Loading] = useState(false);
  const [id3Loaded, setId3Loaded] = useState(0);
  const [id3Total, setId3Total] = useState(0);
  const id3BatchSize = 50;
  const [view, setView] = useState<'lyrics' | 'settings' | 'collection'>('lyrics');
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode>('continuous');
  const [showCover, setShowCover] = useState(true);
  const [showInfo, setShowInfo] = useState(false);
  const [theme, setTheme] = useState<AppTheme>(loadTheme);
  const lastLoggedFile = useRef<string | null>(null);
  const currentFileRef = useRef(currentFile);
  currentFileRef.current = currentFile;
  const playlistRef = useRef(playlistFiles);
  playlistRef.current = playlistFiles;
  const modeRef = useRef(playbackMode);
  modeRef.current = playbackMode;
  const intentionalStopRef = useRef(false);

  const handleLoadPlaylist = useCallback(async (folder: string, forceRefresh = false): Promise<boolean> => {
    intentionalStopRef.current = true;
    try {
      const res = await fetch(`${API}/playlist?path=${encodeURIComponent(folder)}`);
      if (res.ok) {
        const files: string[] = await res.json();
        setPlaylistFiles(files);
        setLibraryFiles(files);
        setCurrentFile(null);
        setStatus('stopped');
        setPosition(0);
        setDuration(0);
        setId3Cache(new Map());
        localStorage.setItem(STORAGE_KEY, folder);
        if (files.length > 0) {
          setId3Loading(true);
          setId3Total(files.length);
          setId3Loaded(0);
          try {
            for (let i = 0; i < files.length; i += id3BatchSize) {
              const batch = files.slice(i, i + id3BatchSize);
              const id3Res = await fetch(`${API}/id3/bulk?refresh=${forceRefresh}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(batch),
              });
              if (id3Res.ok) {
                const tagsMap: Record<string, Id3Tags> = await id3Res.json();
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
      }
      return false;
    } catch (_) {
      return false;
    }
  }, []);

  const ensureId3For = useCallback(async (files: string[]) => {
    const missing = files.filter(f => !id3Cache.has(f));
    if (missing.length === 0) return;
    
    setId3Total(prev => Math.max(prev, missing.length));
    
    for (let i = 0; i < missing.length; i += id3BatchSize) {
      const batch = missing.slice(i, i + id3BatchSize);
      const id3Res = await fetch(`${API}/id3/bulk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(batch),
      });
      if (id3Res.ok) {
        const tagsMap: Record<string, Id3Tags> = await id3Res.json();
        setId3Cache(prev => {
          const next = new Map(prev);
          for (const [k, v] of Object.entries(tagsMap)) next.set(k, v);
          return next;
        });
        setId3Loaded(prev => prev + batch.length);
      }
    }
  }, [id3Cache, id3BatchSize]);

  const loadVirtualPlaylist = useCallback(async (name: string): Promise<boolean> => {
    intentionalStopRef.current = true;
    try {
      const res = await fetch(`${API}/playlist/${encodeURIComponent(name)}`);
      if (!res.ok) return false;
      const files: string[] = await res.json();
      setPlaylistFiles(files);
      setCurrentFile(null);
      setStatus('stopped');
      setPosition(0);
      setDuration(0);
      if (files.length > 0) {
        setId3Loading(true);
        setId3Total(files.length);
        try {
          await ensureId3For([...new Set([...libraryFiles, ...files])]);
        } finally {
          setId3Loading(false);
        }
      }
      return true;
    } catch (_) {
      return false;
    }
  }, [libraryFiles, ensureId3For]);

  const refreshPlaylists = useCallback(async () => {
    try {
      const res = await fetch(`${API}/playlists`);
      if (res.ok) setPlaylists(await res.json());
    } catch (_) { /* ignore */ }
  }, []);

  useEffect(() => {
    document.body.dataset.theme = theme;
    localStorage.setItem(THEME_KEY, theme);
  }, [theme]);

  useEffect(() => {
    refreshPlaylists();
  }, [refreshPlaylists]);

  useEffect(() => {
    let cancelled = false;
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return;
    let attempts = 0;
    const attempt = () => {
      handleLoadPlaylist(saved).then(ok => {
        if (!cancelled && !ok && attempts < 15) {
          attempts++;
          setTimeout(attempt, 1000);
        }
      });
    };
    attempt();
    return () => { cancelled = true; };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let cancelled = false;
    let prevStatus: Status = 'stopped';
    (async function poll() {
      while (!cancelled) {
        try {
          const res = await fetch(`${API}/playing`);
          if (res.ok) {
            const data: PlayingData = await res.json();
            if (data.status === 'stopped') {
              setStatus('stopped');
              setPosition(0);
              setDuration(0);
              lastLoggedFile.current = null;

              if ((prevStatus === 'playing' || prevStatus === 'paused') && !intentionalStopRef.current) {
                const next = getNextFile(currentFileRef.current, playlistRef.current, modeRef.current);
                if (next) {
                  fetch(`${API}/play`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'text/plain' },
                    body: next,
                  }).then(r => {
                    if (r.ok) {
                      setCurrentFile(next);
                      setStatus('playing');
                    }
                  });
                }
              }
              intentionalStopRef.current = false;
            } else {
              setStatus(data.status);
              setPosition(data.position);
              setDuration(data.duration);
              const tags = data.id3;
              if (data.file && tags) {
                setId3Cache(prev => {
                  const next = new Map(prev);
                  next.set(data.file, tags);
                  return next;
                });
              }
              if (tags && data.file !== lastLoggedFile.current) {
                lastLoggedFile.current = data.file;
                console.log('--- ID3 Tags ---');
                for (const [k, v] of Object.entries(tags)) {
                  console.log(`${k}: ${v}`);
                }
                console.log('-----------------');
              }
            }
            prevStatus = data.status;
          }
        } catch (_) { /* ignore */ }
        await new Promise(r => setTimeout(r, 2000));
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const playFile = useCallback(async (file: string) => {
    try {
      const res = await fetch(`${API}/play`, {
        method: 'POST',
        headers: { 'Content-Type': 'text/plain' },
        body: file,
      });
      if (res.ok) {
        setCurrentFile(file);
        setStatus('playing');
      }
    } catch (_) { /* ignore */ }
  }, []);

  const handlePause = useCallback(async () => {
    try {
      await fetch(`${API}/pause`, { method: 'POST' });
      setStatus('paused');
    } catch (_) { /* ignore */ }
  }, []);

  const handleResume = useCallback(async () => {
    try {
      await fetch(`${API}/resume`, { method: 'POST' });
      setStatus('playing');
    } catch (_) { /* ignore */ }
  }, []);

  const handleTogglePlayPause = useCallback(() => {
    if (status === 'stopped' && currentFile) {
      playFile(currentFile);
    } else if (status === 'paused') {
      handleResume();
    } else if (status === 'playing') {
      handlePause();
    }
  }, [status, currentFile, playFile, handleResume, handlePause]);

  const handleStop = useCallback(async () => {
    intentionalStopRef.current = true;
    try {
      await fetch(`${API}/stop`, { method: 'POST' });
      setCurrentFile(null);
      setStatus('stopped');
      setPosition(0);
      setDuration(0);
    } catch (_) { /* ignore */ }
  }, []);

  const handlePrev = useCallback(() => {
    const target = getPrevFile(currentFile, playlistFiles, playbackMode);
    if (target) playFile(target);
  }, [currentFile, playlistFiles, playbackMode, playFile]);

  const handleNext = useCallback(() => {
    const target = getNextFile(currentFile, playlistFiles, playbackMode);
    if (target) playFile(target);
  }, [currentFile, playlistFiles, playbackMode, playFile]);

  const handleSeek = useCallback(async (positionMs: number) => {
    try {
      await fetch(`${API}/seek`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ position: positionMs }),
      });
      setPosition(positionMs);
    } catch (_) { /* ignore */ }
  }, []);

  const currentId3 = currentFile ? id3Cache.get(currentFile) : undefined;

  const handleOpenSettings = useCallback(() => {
    setView('settings');
  }, []);

  const handleOpenLyrics = useCallback(() => {
    setView('lyrics');
  }, []);

  const handleOpenCollection = useCallback(() => {
    setView('collection');
  }, []);

  const handleOpenInfo = useCallback(() => {
    setShowInfo(true);
  }, []);

  const handleCloseInfo = useCallback(() => {
    setShowInfo(false);
  }, []);

  const handleTagsUpdated = useCallback((file: string, tags: Id3Tags) => {
    setId3Cache(prev => {
      const next = new Map(prev);
      next.set(file, tags);
      return next;
    });
  }, []);

  const handleSaveFiltered = useCallback(async (filteredFiles: string[], name: string): Promise<boolean> => {
    try {
      const res = await fetch(`${API}/playlist`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, paths: filteredFiles }),
      });
      if (res.ok) {
        await refreshPlaylists();
        return true;
      }
      return false;
    } catch {
      return false;
    }
  }, [refreshPlaylists]);

  const handleLoadAll = useCallback(() => {
    setPlaylistFiles(libraryFiles);
    setCurrentFile(null);
    setStatus('stopped');
    setPosition(0);
    setDuration(0);
  }, [libraryFiles]);

  const handleScrollToCurrent = useCallback(() => {
    const active = document.querySelector('#playlist li.active');
    if (active) {
      active.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, []);

  const coverUrl = currentFile ? `${API}/cover?path=${encodeURIComponent(currentFile)}` : null;

  return (
    <div id="app">
      <Toolbar
        view={view}
        onOpenSettings={handleOpenSettings}
        onOpenLyrics={handleOpenLyrics}
        onOpenCollection={handleOpenCollection}
        onOpenInfo={handleOpenInfo}
      />

      <div id="main-content">
        {view === 'lyrics' ? (
          <LyricsPanel currentFile={currentFile} />
        ) : view === 'settings' ? (
          <SettingsPanel
            playbackMode={playbackMode}
            showCover={showCover}
            theme={theme}
            onPlaybackModeChange={setPlaybackMode}
            onShowCoverChange={setShowCover}
            onThemeChange={setTheme}
            onLoadPlaylist={handleLoadPlaylist}
          />
        ) : (
          <CollectionManager
            libraryFiles={libraryFiles}
            id3Cache={id3Cache}
            onTagsUpdated={handleTagsUpdated}
            playlists={playlists}
            onRefreshPlaylists={refreshPlaylists}
            onLoadPlaylist={loadVirtualPlaylist}
            onLoadAll={handleLoadAll}
          />
        )}
        <div id="right-panel">
          <Player
            status={status}
            position={position}
            duration={duration}
            currentFile={currentFile}
            currentId3={currentId3}
            showCover={showCover}
            coverUrl={coverUrl}
            onTogglePlayPause={handleTogglePlayPause}
            onStop={handleStop}
            onPrev={handlePrev}
            onNext={handleNext}
            onSeek={handleSeek}
            onScrollToCurrent={handleScrollToCurrent}
          />
          <Playlist
            files={playlistFiles}
            currentFile={currentFile}
            id3Cache={id3Cache}
            loading={id3Loading}
            id3Loaded={id3Loaded}
            id3Total={id3Total}
            onSelect={playFile}
            onSaveFiltered={handleSaveFiltered}
          />
        </div>
      </div>
      <footer id="statusbar">v1.3.0</footer>
      {showInfo && <InfoModal onClose={handleCloseInfo} />}
    </div>
  );
}
