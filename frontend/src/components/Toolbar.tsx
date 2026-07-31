import LyricsIcon from '@mui/icons-material/Lyrics';
import DisplaySettingsIcon from '@mui/icons-material/DisplaySettings';
import LibraryMusicIcon from '@mui/icons-material/LibraryMusic';
import InfoIcon from '@mui/icons-material/Info';

interface Props {
  view: 'lyrics' | 'settings' | 'collection';
  onOpenSettings: () => void;
  onOpenLyrics: () => void;
  onOpenCollection: () => void;
  onOpenInfo: () => void;
}

export default function Toolbar({ view, onOpenSettings, onOpenLyrics, onOpenCollection, onOpenInfo }: Props) {
  return (
    <div id="toolbar">
      <button
        id="toolbar-lyrics-btn"
        onClick={onOpenLyrics}
        title="Letra da música"
        disabled={view === 'lyrics'}
      >
        <LyricsIcon />
      </button>
      <button
        id="toolbar-collection-btn"
        onClick={onOpenCollection}
        title="Coleção"
        disabled={view === 'collection'}
      >
        <LibraryMusicIcon />
      </button>
      <button
        id="toolbar-settings-btn"
        onClick={onOpenSettings}
        title="Configurações"
        disabled={view === 'settings'}
      >
        <DisplaySettingsIcon />
      </button>
      <button
        id="toolbar-info-btn"
        onClick={onOpenInfo}
        title="Sobre"
      >
        <InfoIcon />
      </button>
    </div>
  );
}
