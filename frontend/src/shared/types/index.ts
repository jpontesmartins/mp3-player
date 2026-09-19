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

export type AppTheme = 'dark' | 'light';

export type PlaybackMode = 'continuous' | 'shuffle' | 'repeat';

export type Status = 'playing' | 'paused' | 'stopped';

export interface PlayingData {
  status: Status;
  file: string;
  position: number;
  duration: number;
  id3?: Id3Tags;
}

export interface DictionaryResult {
  word: string;
  source: string;
  language: string;
  meanings: string;
}
