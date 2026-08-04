import { useState, useCallback, useEffect } from 'react';

const API = 'http://localhost:8111';

interface Props {
  currentFile: string | null;
}

const MIN_SIZE = 0.7;
const MAX_SIZE = 2.0;
const STEP = 0.1;

export default function LyricsPanel({ currentFile }: Props) {
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fontSize, setFontSize] = useState(0.9);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');

  useEffect(() => {
    if (!currentFile) {
      setLyrics(null);
      return;
    }
    setLyrics(null);
    setEditing(false);
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

  const handleStartEdit = useCallback(() => {
    setDraft(lyrics ?? '');
    setEditing(true);
  }, [lyrics]);

  const handleCancelEdit = useCallback(() => {
    setEditing(false);
  }, []);

  const handleSave = useCallback(async () => {
    if (!currentFile) return;
    setSaving(true);
    try {
      const res = await fetch(`${API}/lyrics`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ path: currentFile, text: draft }),
      });
      if (res.ok) {
        setLyrics(draft);
        setEditing(false);
      } else {
        alert(`Erro ao salvar letra: ${await res.text()}`);
      }
    } catch {
      alert('Erro ao conectar com o servidor');
    } finally {
      setSaving(false);
    }
  }, [currentFile, draft]);

  const handleDelete = useCallback(async () => {
    if (!currentFile) return;
    if (!confirm('Remover letra salva?')) return;
    try {
      const res = await fetch(`${API}/lyrics?path=${encodeURIComponent(currentFile)}`, {
        method: 'DELETE',
      });
      if (res.ok) {
        setLyrics(null);
      } else {
        alert(`Erro ao remover letra: ${await res.text()}`);
      }
    } catch {
      alert('Erro ao conectar com o servidor');
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
            {editing ? (
              <>
                <button id="save-lyrics-btn" onClick={handleSave} disabled={saving}>
                  {saving ? 'Salvando...' : 'Salvar'}
                </button>
                <button id="cancel-edit-btn" onClick={handleCancelEdit}>Cancelar</button>
              </>
            ) : (
              <>
                <button id="fetch-lyrics-btn" onClick={handleFetch} disabled={loading}>
                  {loading ? 'Buscando...' : 'Buscar letra'}
                </button>
                <button id="edit-lyrics-btn" onClick={handleStartEdit}>Alterar letra</button>
                <button id="delete-lyrics-btn" onClick={handleDelete}>Remover letra</button>
              </>
            )}
            <div className="lyrics-font-controls">
              <button className="font-btn" onClick={() => setFontSize(s => Math.min(MAX_SIZE, s + STEP))} disabled={fontSize >= MAX_SIZE}>A+</button>
              <button className="font-btn" onClick={() => setFontSize(s => Math.max(MIN_SIZE, s - STEP))} disabled={fontSize <= MIN_SIZE}>A-</button>
            </div>
          </div>
          {editing ? (
            <textarea
              className="lyrics-editor"
              style={{ fontSize: `${fontSize}rem` }}
              value={draft}
              onChange={e => setDraft(e.target.value)}
              spellCheck={false}
            />
          ) : (
            <pre className="lyrics-text" style={{ fontSize: `${fontSize}rem` }}>{lyrics}</pre>
          )}
        </div>
      )}
    </section>
  );
}
