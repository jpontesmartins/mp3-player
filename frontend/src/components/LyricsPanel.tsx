import { useState, useCallback, useEffect } from 'react';

const API = 'http://localhost:8080';

interface Props {
  currentFile: string | null;
}

const MIN_SIZE = 0.7;
const MAX_SIZE = 2.0;
const STEP = 0.1;

export default function LyricsPanel({ currentFile }: Props) {
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [fontSize, setFontSize] = useState(0.9);

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
            <div className="lyrics-font-controls">
              <button className="font-btn" onClick={() => setFontSize(s => Math.min(MAX_SIZE, s + STEP))} disabled={fontSize >= MAX_SIZE}>A+</button>
              <button className="font-btn" onClick={() => setFontSize(s => Math.max(MIN_SIZE, s - STEP))} disabled={fontSize <= MIN_SIZE}>A-</button>
            </div>
          </div>
          <pre className="lyrics-text" style={{ fontSize: `${fontSize}rem` }}>{lyrics}</pre>
        </div>
      )}
    </section>
  );
}
