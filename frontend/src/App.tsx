import { useState, useEffect, useCallback, useRef } from 'react';
import FolderSelector from './components/FolderSelector';
import Player from './components/Player';
import Playlist from './components/Playlist';
import LyricsPanel from './components/LyricsPanel';
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

export default function App() {
  const [playlistFiles, setPlaylistFiles] = useState<string[]>([]);
  const [currentFile, setCurrentFile] = useState<string | null>(null);
  const [status, setStatus] = useState<Status>('stopped');
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [id3Cache, setId3Cache] = useState<Map<string, Id3Tags>>(new Map());
  const lastLoggedFile = useRef<string | null>(null);

  const fetchId3ForFile = useCallback(async (file: string) => {
    if (id3Cache.has(file)) return;
    try {
      const res = await fetch(`${API}/id3?path=${encodeURIComponent(file)}`);
      if (res.ok) {
        const tags: Id3Tags = await res.json();
        setId3Cache(prev => {
          const next = new Map(prev);
          next.set(file, tags);
          return next;
        });
      }
    } catch (_) { /* ignore */ }
  }, [id3Cache]);

  const handleLoadPlaylist = useCallback(async (folder: string) => {
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
        files.forEach(f => fetchId3ForFile(f));
      }
    } catch (_) { /* ignore */ }
  }, [fetchId3ForFile]);

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      handleLoadPlaylist(saved);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let cancelled = false;
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

  const handlePlayPlaylist = useCallback(() => {
    if (currentFile) playFile(currentFile);
  }, [currentFile, playFile]);

  const currentId3 = currentFile ? id3Cache.get(currentFile) : undefined;

  return (
    <div id="app">
      <header>
        <h1>MP3 Player</h1>
      </header>

      <FolderSelector onLoad={handleLoadPlaylist} />

      <div id="main-content">
        <LyricsPanel />
        <div id="right-panel">
          <Player
            status={status}
            position={position}
            duration={duration}
            currentFile={currentFile}
            currentId3={currentId3}
            onPlay={handlePlayPlaylist}
            onPause={handlePause}
            onResume={handleResume}
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
