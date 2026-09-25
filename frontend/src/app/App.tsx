import { useEffect } from 'react';
import { useApp } from './providers/AppContext';
import { useLibrary } from './providers/LibraryContext';
import { useLoadFolder } from '../features/load-folder/lib/useLoadFolder';
import { usePlaybackPolling } from '../features/playback/lib/usePlaybackPolling';
import Toolbar from '../widgets/Toolbar';
import LeftPanel from '../widgets/LeftPanel';
import RightPanel from '../widgets/RightPanel';
import InfoModal from '../features/app-info/ui/InfoModal';
import './App.css';

const STORAGE_KEY = 'mp3_folder';

export default function App() {
  const app = useApp();
  const library = useLibrary();
  const { restoreLastFolder } = useLoadFolder();

  usePlaybackPolling();

  useEffect(() => {
    library.refreshPlaylists();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    let cancelled = false;
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return;
    let attempts = 0;
    const attempt = () => {
      restoreLastFolder().then(success => {
        if (!cancelled && !success && attempts < 15) {
          attempts++;
          setTimeout(attempt, 1000);
        }
      });
    };
    attempt();
    return () => { cancelled = true; };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div id="app">
      <Toolbar />
      <div id="main-content">
        <LeftPanel />
        <RightPanel />
      </div>
      <footer id="statusbar">v1.5.2</footer>
      {app.showInfo && <InfoModal />}
    </div>
  );
}
