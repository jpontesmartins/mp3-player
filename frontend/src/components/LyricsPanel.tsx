import { useState, useCallback, useEffect } from 'react';

const API = 'http://localhost:8080';

interface Props {
  currentFile: string | null;
}

export default function LyricsPanel({ currentFile }: Props) {
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!currentFile) {
      setLyrics(null);
      return;
    }
    setLyrics(null);
    (async () => {
      try {
        const res = await fetch(`${API}/lyrics/cached?path=${encodeURIComponent(currentFile)}`);
        if (res.ok) {
          setLyrics(await res.text());
        }
      } catch {}
    })();
  }, [currentFile]);

  const handleFetch = useCallback(async () => {
    if (!currentFile) return;
    setLoading(true);
    setLyrics(null);
    try {
      const res = await fetch(`${API}/lyrics?path=${encodeURIComponent(currentFile)}`);
      if (res.ok) {
        setLyrics(await res.text());
      } else {
        setLyrics(`Erro: ${await res.text()}`);
      }
    } catch {
      setLyrics('Erro ao conectar com o servidor');
    } finally {
      setLoading(false);
    }
  }, [currentFile]);

  const canFetch = !!currentFile;

  return (
    <section id="lyrics-panel">
      {lyrics === null ? (
        <div className="lyrics-empty">
          <button id="fetch-lyrics-btn" onClick={handleFetch} disabled={!canFetch || loading}>
            {loading ? 'Buscando...' : 'Buscar letra'}
          </button>
          {!canFetch && <p className="lyrics-placeholder">Nenhuma música selecionada</p>}
        </div>
      ) : (
        <div className="lyrics-content">
          <div className="lyrics-header">
            <button id="fetch-lyrics-btn" onClick={handleFetch} disabled={loading}>
              {loading ? 'Buscando...' : 'Buscar letra'}
            </button>
          </div>
          <pre className="lyrics-text">{lyrics}</pre>
        </div>
      )}
    </section>
  );
}
