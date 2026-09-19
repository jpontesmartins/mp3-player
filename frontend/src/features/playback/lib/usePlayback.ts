import { useCallback } from 'react';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import * as playbackApi from '../../../shared/api/playback';
import { getNextFile } from './navigation';

const MAX_HISTORY = 100;

export function usePlayback() {
  const player = usePlayer();
  const library = useLibrary();

  const playFile = useCallback(async (file: string) => {
    if (player.currentFile && player.currentFile !== file) {
      await playbackApi.stop();
    }
    const success = await playbackApi.play(file);
    if (success) {
      if (player.currentFile && player.currentFile !== file) {
        const newHistory = [...player.playHistoryRef.current, player.currentFile].slice(-MAX_HISTORY);
        player.setPlayHistory(newHistory);
      }
      player.setCurrentFile(file);
      player.setStatus('playing');
    }
  }, [player]);

  const pause = useCallback(async () => {
    const success = await playbackApi.pause();
    if (success) player.setStatus('paused');
  }, [player]);

  const resume = useCallback(async () => {
    const success = await playbackApi.resume();
    if (success) player.setStatus('playing');
  }, [player]);

  const stop = useCallback(async () => {
    player.setIntentionalStop(true);
    const success = await playbackApi.stop();
    if (success) {
      player.setStatus('paused');
      player.setPosition(0);
    }
  }, [player]);

  const seek = useCallback(async (positionMilliseconds: number) => {
    const success = await playbackApi.seek(positionMilliseconds);
    if (success) player.setPosition(positionMilliseconds);
  }, [player]);

  const togglePlayPause = useCallback(async () => {
    if (player.status === 'stopped' && player.currentFile) {
      await playFile(player.currentFile);
    } else if (player.status === 'paused') {
      await resume();
    } else if (player.status === 'playing') {
      await pause();
    }
  }, [player.status, player.currentFile, playFile, resume, pause]);

  const previous = useCallback(() => {
    if (player.playHistory.length > 0) {
      const prevFile = player.playHistory[player.playHistory.length - 1];
      player.setPlayHistory(player.playHistory.slice(0, -1));
      playFile(prevFile);
    }
  }, [player.playHistory, player.setPlayHistory, playFile]);

  const next = useCallback(() => {
    const target = getNextFile(player.currentFile, library.playlistFiles, player.playbackMode);
    if (target) playFile(target);
  }, [player.currentFile, library.playlistFiles, player.playbackMode, playFile]);

  const scrollToCurrent = useCallback(() => {
    const active = document.querySelector('#playlist li.active');
    if (active) active.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, []);

  return { playFile, pause, resume, stop, seek, togglePlayPause, previous, next, scrollToCurrent };
}
