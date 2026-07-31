import { useState } from 'react';

export type PlaybackMode = 'continuous' | 'shuffle' | 'repeat';

interface Props {
  playbackMode: PlaybackMode;
  showCover: boolean;
  onPlaybackModeChange: (mode: PlaybackMode) => void;
  onShowCoverChange: (show: boolean) => void;
  onLoadPlaylist: (path: string) => Promise<boolean>;
}

export default function SettingsPanel({ playbackMode, showCover, onPlaybackModeChange, onShowCoverChange, onLoadPlaylist }: Props) {
  const [path, setPath] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLoad = async () => {
    if (!path.trim()) return;
    setLoading(true);
    await onLoadPlaylist(path.trim());
    setLoading(false);
  };

  return (
    <section id="settings-panel">
      <div className="settings-content">
        <h2 className="settings-title">Configurações</h2>

        <div className="settings-group">
          <label className="settings-label">Tipo de reprodução</label>
          <div className="settings-options">
            <label className={`settings-radio ${playbackMode === 'continuous' ? 'active' : ''}`}>
              <input
                type="radio"
                name="playback"
                value="continuous"
                checked={playbackMode === 'continuous'}
                onChange={() => onPlaybackModeChange('continuous')}
              />
              Contínua
            </label>
            <label className={`settings-radio ${playbackMode === 'shuffle' ? 'active' : ''}`}>
              <input
                type="radio"
                name="playback"
                value="shuffle"
                checked={playbackMode === 'shuffle'}
                onChange={() => onPlaybackModeChange('shuffle')}
              />
              Aleatória
            </label>
            <label className={`settings-radio ${playbackMode === 'repeat' ? 'active' : ''}`}>
              <input
                type="radio"
                name="playback"
                value="repeat"
                checked={playbackMode === 'repeat'}
                onChange={() => onPlaybackModeChange('repeat')}
              />
              Repetição
            </label>
          </div>
        </div>

        <div className="settings-group">
          <label className="settings-checkbox">
            <input
              type="checkbox"
              checked={showCover}
              onChange={e => onShowCoverChange(e.target.checked)}
            />
            Habilitar mostrar capa do álbum
          </label>
        </div>

        <div className="settings-divider" />

        <div className="settings-group">
          <label className="settings-label">Selecionar pasta da playlist</label>
          <div className="settings-folder-row">
            <input
              type="text"
              id="settings-folder-input"
              placeholder="C:\Users\joao_\Music"
              value={path}
              onChange={e => setPath(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleLoad(); }}
            />
            <button id="settings-load-btn" onClick={handleLoad} disabled={loading}>
              {loading ? 'Carregando...' : 'Carregar'}
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
