import { useState, useEffect } from 'react';
import { useApp } from '../../../app/providers/AppContext';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLoadFolder } from '../../load-folder/lib/useLoadFolder';
import { getInfo, type SystemInfo } from '../../../shared/api/system';

export default function SettingsPanel() {
  const app = useApp();
  const player = usePlayer();
  const { loadFolderWithRetry } = useLoadFolder();
  const [path, setPath] = useState('');
  const [loading, setLoading] = useState(false);
  const [info, setInfo] = useState<SystemInfo | null>(null);

  useEffect(() => {
    let cancelled = false;
    getInfo().then((data: SystemInfo | null) => { if (!cancelled && data) setInfo(data); });
    return () => { cancelled = true; };
  }, []);

  const handleLoad = async () => {
    if (!path.trim()) return;
    setLoading(true);
    await loadFolderWithRetry(path.trim(), true);
    setLoading(false);
  };

  return (
    <section id="settings-panel">
      <div className="settings-content">
        <h2 className="settings-title">Configurações</h2>

        <div className="settings-group">
          <label className="settings-label">Tema</label>
          <div className="settings-options">
            <label className={`settings-radio ${app.theme === 'dark' ? 'active' : ''}`}>
              <input type="radio" name="theme" value="dark" checked={app.theme === 'dark'} onChange={() => app.setTheme('dark')} />Escuro
            </label>
            <label className={`settings-radio ${app.theme === 'light' ? 'active' : ''}`}>
              <input type="radio" name="theme" value="light" checked={app.theme === 'light'} onChange={() => app.setTheme('light')} />Claro
            </label>
          </div>
        </div>

        <div className="settings-group">
          <label className="settings-label">Tipo de reprodução</label>
          <div className="settings-options">
            <label className={`settings-radio ${player.playbackMode === 'continuous' ? 'active' : ''}`}>
              <input type="radio" name="playback" value="continuous" checked={player.playbackMode === 'continuous'} onChange={() => player.setPlaybackMode('continuous')} />Contínua
            </label>
            <label className={`settings-radio ${player.playbackMode === 'shuffle' ? 'active' : ''}`}>
              <input type="radio" name="playback" value="shuffle" checked={player.playbackMode === 'shuffle'} onChange={() => player.setPlaybackMode('shuffle')} />Aleatória
            </label>
            <label className={`settings-radio ${player.playbackMode === 'repeat' ? 'active' : ''}`}>
              <input type="radio" name="playback" value="repeat" checked={player.playbackMode === 'repeat'} onChange={() => player.setPlaybackMode('repeat')} />Repetição
            </label>
          </div>
        </div>

        <div className="settings-group">
          <label className="settings-checkbox">
            <input type="checkbox" checked={player.showCover} onChange={e => player.setShowCover(e.target.checked)} />
            Habilitar mostrar capa do álbum
          </label>
        </div>

        <div className="settings-divider" />

        <div className="settings-group">
          <label className="settings-label">Selecionar pasta da playlist</label>
          <div className="settings-folder-row">
            <input type="text" id="settings-folder-input" placeholder="C:\Users\joao_\Music" value={path} onChange={e => setPath(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') handleLoad(); }} />
            <button id="settings-load-btn" onClick={handleLoad} disabled={loading}>{loading ? 'Carregando...' : 'Carregar'}</button>
          </div>
        </div>

        <div className="settings-divider" />

        <div className="settings-group">
          <label className="settings-label">Informações do sistema</label>
          <dl className="settings-info">
            <div className="settings-info-row"><dt>Local do log do backend</dt><dd className="settings-info-path">{info?.logFile || '—'}</dd></div>
            <div className="settings-info-row"><dt>Local do cache de metadados</dt><dd className="settings-info-path">{info?.cacheFile || '—'}</dd></div>
            <div className="settings-info-row"><dt>Porta do backend</dt><dd>{info?.backendPort || '8111'}</dd></div>
            <div className="settings-info-row"><dt>Porta do frontend</dt><dd>{info?.frontendPort || '8112'}</dd></div>
          </dl>
        </div>
      </div>
    </section>
  );
}
