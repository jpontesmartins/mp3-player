import { useEffect, useRef } from 'react';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLibrary } from '../../../app/providers/LibraryContext';
import * as playbackApi from '../../../shared/api/playback';
import { getNextFile } from './navigation';

export function usePlaybackPolling() {
  const player = usePlayer();
  const library = useLibrary();
  const lastLoggedFile = useRef<string | null>(null);
  const prevStatusRef = useRef(player.status);
  prevStatusRef.current = player.status;

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

            if ((prevStatusRef.current === 'playing' || prevStatusRef.current === 'paused') && !player.intentionalStop) {
              const next = getNextFile(player.currentFileRef.current, library.playlistFiles, player.modeRef.current);
              if (next) {
                const ok = await playbackApi.play(next);
                if (ok) {
                  player.setCurrentFile(next);
                  player.setStatus('playing');
                }
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
              for (const [k, v] of Object.entries(tags)) {
                console.log(`${k}: ${v}`);
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

function delay(ms: number) {
  return new Promise(r => setTimeout(r, ms));
}
