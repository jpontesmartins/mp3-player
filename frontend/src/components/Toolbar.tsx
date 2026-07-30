interface Props {
  view: 'lyrics' | 'settings';
  onOpenSettings: () => void;
  onOpenLyrics: () => void;
}

export default function Toolbar({ view, onOpenSettings, onOpenLyrics }: Props) {
  return (
    <div id="toolbar">
      <button
        id="toolbar-lyrics-btn"
        onClick={onOpenLyrics}
        title="Letra da música"
        disabled={view === 'lyrics'}
      >
        📃
      </button>
      <button
        id="toolbar-settings-btn"
        onClick={onOpenSettings}
        title="Configurações"
        disabled={view === 'settings'}
      >
        ⚙️
      </button>
    </div>
  );
}
