import { useEffect, useRef } from 'react';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import * as playbackApi from '../../../shared/api/playback';
import { getNextFile } from './navigation';

export function usePlaybackPolling() {
  const player = usePlayer();
  const library = useLibrary();
  const lastLoggedFile = useRef<string | null>(null);
  const previousStatusRef = useRef(player.status);
  previousStatusRef.current = player.status;
  const advancingRef = useRef(false);

  useEffect(() => {
    let cancelled = false;
    (async function poll() {
      while (!cancelled) {
        try {
          const data = await playbackApi.getStatus();
          if (!data) { await delay(2000); continue; }

          if (data.status === 'stopped') {
            player.setStatus('stopped');
            player.setPosition(0);
            player.setDuration(0);
            lastLoggedFile.current = null;

            if ((previousStatusRef.current === 'playing' || previousStatusRef.current === 'paused') && !player.intentionalStop && !advancingRef.current) {
              advancingRef.current = true;
              try {
                const nextTrack = getNextFile(player.currentFileRef.current, library.playlistFiles, player.modeRef.current);
                if (nextTrack) {
                  const success = await playbackApi.play(nextTrack);
                  if (success) {
                    player.setCurrentFile(nextTrack);
                    player.setStatus('playing');
                  }
                }
              } finally {
                advancingRef.current = false;
              }
            }
            player.setIntentionalStop(false);
          } else {
            player.setStatus(data.status);
            player.setPosition(data.position);
            player.setDuration(data.duration);
            const tags = data.id3;
            if (data.file && tags) {
              library.updateId3Cache(data.file, tags);
            }
            if (tags && data.file !== lastLoggedFile.current) {
              lastLoggedFile.current = data.file;
              console.log('--- ID3 Tags ---');
              for (const [fieldKey, fieldValue] of Object.entries(tags)) {
                console.log(`${fieldKey}: ${fieldValue}`);
              }
              console.log('-----------------');
            }
          }
        } catch { /* ignore */ }
        await delay(2000);
      }
    })();
    return () => { cancelled = true; };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps
}

function delay(milliseconds: number) {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
}
