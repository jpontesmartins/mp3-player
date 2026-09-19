import { createContext, useContext, useState, useRef } from 'react';
import type { ReactNode } from 'react';
import type { PlaybackMode } from '../../shared/types';

interface PlayerState {
  currentFile: string | null;
  status: 'playing' | 'paused' | 'stopped';
  position: number;
  duration: number;
  playbackMode: PlaybackMode;
  showCover: boolean;
  intentionalStop: boolean;
  playHistory: string[];
}

interface PlayerActions {
  setCurrentFile: (file: string | null) => void;
  setStatus: (status: PlayerState['status']) => void;
  setPosition: (position: number) => void;
  setDuration: (duration: number) => void;
  setPlaybackMode: (mode: PlaybackMode) => void;
  setShowCover: (show: boolean) => void;
  setIntentionalStop: (value: boolean) => void;
  setPlayHistory: (history: string[]) => void;
  currentFileRef: React.MutableRefObject<string | null>;
  playlistRef: React.MutableRefObject<string[]>;
  modeRef: React.MutableRefObject<PlaybackMode>;
  playHistoryRef: React.MutableRefObject<string[]>;
}

const PlayerContext = createContext<PlayerState & PlayerActions>(null!);

export function PlayerProvider({ children }: { children: ReactNode }) {
  const [currentFile, setCurrentFile] = useState<string | null>(null);
  const [status, setStatus] = useState<'playing' | 'paused' | 'stopped'>('stopped');
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode>('continuous');
  const [showCover, setShowCover] = useState(true);
  const [intentionalStop, setIntentionalStop] = useState(false);
  const [playHistory, setPlayHistory] = useState<string[]>([]);

  const currentFileRef = useRef(currentFile);
  currentFileRef.current = currentFile;
  const playlistRef = useRef<string[]>([]);
  const modeRef = useRef(playbackMode);
  modeRef.current = playbackMode;
  const playHistoryRef = useRef(playHistory);
  playHistoryRef.current = playHistory;

  return (
    <PlayerContext.Provider value={{
      currentFile, status, position, duration, playbackMode, showCover, intentionalStop, playHistory,
      setCurrentFile, setStatus, setPosition, setDuration, setPlaybackMode, setShowCover, setIntentionalStop, setPlayHistory,
      currentFileRef, playlistRef, modeRef, playHistoryRef,
    }}>
      {children}
    </PlayerContext.Provider>
  );
}

export function usePlayer() {
  return useContext(PlayerContext);
}
