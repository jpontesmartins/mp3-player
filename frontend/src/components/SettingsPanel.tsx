import { useState, useEffect } from 'react';
import type { AppTheme } from '../App';

export type PlaybackMode = 'continuous' | 'shuffle' | 'repeat';

interface Props {
  playbackMode: PlaybackMode;
  showCover: boolean;
  theme: AppTheme;
  onPlaybackModeChange: (mode: PlaybackMode) => void;
  onShowCoverChange: (show: boolean) => void;
  onThemeChange: (theme: AppTheme) => void;
  onLoadPlaylist: (path: string) => Promise<boolean>;
}

interface SystemInfo {
  logFile: string;
  backendPort: string;
  frontendPort: string;
}

const API = 'http://localhost:8111';

export default function SettingsPanel({ playbackMode, showCover, theme, onPlaybackModeChange, onShowCoverChange, onThemeChange, onLoadPlaylist }: Props) {
  const [path, setPath] = useState('');
  const [loading, setLoading] = useState(false);
  const [info, setInfo] = useState<SystemInfo | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API}/info`)
      .then(res => (res.ok ? res.json() : null))
      .then((data: SystemInfo | null) => {
        if (!cancelled && data) setInfo(data);
      })
      .catch(() => { /* servidor indisponível */ });
    return () => { cancelled = true; };
  }, []);

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
          <label className="settings-label">Tema</label>
          <div className="settings-options">
            <label className={`settings-radio ${theme === 'dark' ? 'active' : ''}`}>
              <input
                type="radio"
                name="theme"
                value="dark"
                checked={theme === 'dark'}
                onChange={() => onThemeChange('dark')}
              />
              Escuro
            </label>
            <label className={`settings-radio ${theme === 'light' ? 'active' : ''}`}>
              <input
                type="radio"
                name="theme"
                value="light"
                checked={theme === 'light'}
                onChange={() => onThemeChange('light')}
              />
              Claro
            </label>
          </div>
        </div>

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

        <div className="settings-divider" />

        <div className="settings-group">
          <label className="settings-label">Informações do sistema</label>
          <dl className="settings-info">
            <div className="settings-info-row">
              <dt>Local do log do backend</dt>
              <dd className="settings-info-path">{info?.logFile || '—'}</dd>
            </div>
            <div className="settings-info-row">
              <dt>Porta do backend</dt>
              <dd>{info?.backendPort || '8111'}</dd>
            </div>
            <div className="settings-info-row">
              <dt>Porta do frontend</dt>
              <dd>{info?.frontendPort || '8112'}</dd>
            </div>
          </dl>
        </div>
      </div>
    </section>
  );
}
