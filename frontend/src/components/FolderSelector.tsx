import { useState } from 'react';

interface Props {
  onLoad: (path: string) => Promise<boolean>;
}

export default function FolderSelector({ onLoad }: Props) {
  const [path, setPath] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLoad = async () => {
    if (!path.trim()) return;
    setLoading(true);
    await onLoad(path.trim());
    setLoading(false);
  };

  return (
    <section id="folder-section">
      <input
        type="text"
        id="folder-input"
        placeholder="C:\Users\joao_\Music"
        value={path}
        onChange={e => setPath(e.target.value)}
        onKeyDown={e => { if (e.key === 'Enter') handleLoad(); }}
      />
      <button id="load-btn" onClick={handleLoad} disabled={loading}>
        {loading ? 'Carregando...' : 'Carregar'}
      </button>
    </section>
  );
}
