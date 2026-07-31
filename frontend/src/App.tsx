import { useState, useEffect, useCallback, useRef } from 'react';
import Toolbar from './components/Toolbar';
import Player from './components/Player';
import Playlist from './components/Playlist';
import LyricsPanel from './components/LyricsPanel';
import SettingsPanel from './components/SettingsPanel';
import type { PlaybackMode } from './components/SettingsPanel';
import './App.css';

const API = 'http://localhost:8080';
const STORAGE_KEY = 'mp3_folder';

export interface Id3Tags {
  title?: string;
  artist?: string;
  album?: string;
  year?: string;
  genre?: string;
  track?: string;
  duration_ms?: string;
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
  const [currentFile, setCurrentFile] = useState<string | null>(null);
  const [status, setStatus] = useState<Status>('stopped');
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [id3Cache, setId3Cache] = useState<Map<string, Id3Tags>>(new Map());
  const [view, setView] = useState<'lyrics' | 'settings'>('lyrics');
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode>('continuous');
  const [showCover, setShowCover] = useState(true);
  const lastLoggedFile = useRef<string | null>(null);
  const currentFileRef = useRef(currentFile);
  currentFileRef.current = currentFile;
  const playlistRef = useRef(playlistFiles);
  playlistRef.current = playlistFiles;
  const modeRef = useRef(playbackMode);
  modeRef.current = playbackMode;
  const intentionalStopRef = useRef(false);

  const handleLoadPlaylist = useCallback(async (folder: string) => {
    intentionalStopRef.current = true;
    try {
      const res = await fetch(`${API}/playlist?path=${encodeURIComponent(folder)}`);
      if (res.ok) {
        const files: string[] = await res.json();
        setPlaylistFiles(files);
        setCurrentFile(null);
        setStatus('stopped');
        setPosition(0);
        setDuration(0);
        setId3Cache(new Map());
        localStorage.setItem(STORAGE_KEY, folder);
        if (files.length > 0) {
          const id3Res = await fetch(`${API}/id3/bulk`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(files),
          });
          if (id3Res.ok) {
            const tagsMap: Record<string, Id3Tags> = await id3Res.json();
            setId3Cache(new Map(Object.entries(tagsMap)));
          }
        }
      }
    } catch (_) { /* ignore */ }
  }, []);

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      handleLoadPlaylist(saved);
    }
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

  const coverUrl = currentFile ? `${API}/cover?path=${encodeURIComponent(currentFile)}` : null;

  return (
    <div id="app">
      <Toolbar view={view} onOpenSettings={handleOpenSettings} onOpenLyrics={handleOpenLyrics} />

      <div id="main-content">
        {view === 'lyrics' ? (
          <LyricsPanel currentFile={currentFile} />
        ) : (
          <SettingsPanel
            playbackMode={playbackMode}
            showCover={showCover}
            onPlaybackModeChange={setPlaybackMode}
            onShowCoverChange={setShowCover}
            onLoadPlaylist={handleLoadPlaylist}
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
          />
          <Playlist
            files={playlistFiles}
            currentFile={currentFile}
            id3Cache={id3Cache}
            onSelect={playFile}
          />
        </div>
      </div>
    </div>
  );
}
