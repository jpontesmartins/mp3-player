import LyricsIcon from '@mui/icons-material/Lyrics';
import DisplaySettingsIcon from '@mui/icons-material/DisplaySettings';
import LibraryMusicIcon from '@mui/icons-material/LibraryMusic';
import InfoIcon from '@mui/icons-material/Info';
import { useApp } from '../app/providers/AppContext';

export default function Toolbar() {
  const app = useApp();
  return (
    <div id="toolbar">
      <button id="toolbar-lyrics-btn" onClick={() => app.setView('lyrics')} title="Letra da música" disabled={app.view === 'lyrics'}><LyricsIcon /></button>
      <button id="toolbar-collection-btn" onClick={() => app.setView('collection')} title="Coleção" disabled={app.view === 'collection'}><LibraryMusicIcon /></button>
      <button id="toolbar-settings-btn" onClick={() => app.setView('settings')} title="Configurações" disabled={app.view === 'settings'}><DisplaySettingsIcon /></button>
      <button id="toolbar-info-btn" onClick={() => app.setShowInfo(true)} title="Sobre"><InfoIcon /></button>
    </div>
  );
}
