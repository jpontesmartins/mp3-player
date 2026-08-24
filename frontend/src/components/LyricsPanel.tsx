import { useState, useCallback, useEffect, useRef } from 'react';

import { API } from '../config';
import DictionaryModal, { type DictionaryResult } from './DictionaryModal';

interface Props {
  currentFile: string | null;
}

const MIN_SIZE = 0.7;
const MAX_SIZE = 2.0;
const STEP = 0.1;

const FONT_SIZE_KEY = 'lyrics_font_size';

function loadFontSize(): number {
  const saved = localStorage.getItem(FONT_SIZE_KEY);
  if (saved) {
    const n = parseFloat(saved);
    if (!isNaN(n) && n >= MIN_SIZE && n <= MAX_SIZE) return n;
  }
  return 0.9;
}

export default function LyricsPanel({ currentFile }: Props) {
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fontSize, setFontSize] = useState(loadFontSize);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [dictResult, setDictResult] = useState<DictionaryResult | null>(null);
  const [dictLoading, setDictLoading] = useState(false);
  const [ctxMenu, setCtxMenu] = useState<{ x: number; y: number } | null>(null);
  const [selectedWord, setSelectedWord] = useState('');
  const lyricsRef = useRef<HTMLPreElement>(null);
  const ctxMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    localStorage.setItem(FONT_SIZE_KEY, String(fontSize));
  }, [fontSize]);

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

  const handleLyricsContextMenu = useCallback((e: React.MouseEvent<HTMLPreElement>) => {
    const selection = window.getSelection()?.toString().trim();
    if (!selection || selection.includes(' ')) return;
    e.preventDefault();
    setSelectedWord(selection);
    let x = e.clientX + 4;
    let y = e.clientY + 4;
    const menu = ctxMenuRef.current;
    if (menu) {
      const r = menu.getBoundingClientRect();
      if (x + r.width > window.innerWidth) x = e.clientX - r.width - 4;
      if (y + r.height > window.innerHeight) y = e.clientY - r.height - 4;
    }
    setCtxMenu({ x: Math.max(4, x), y: Math.max(4, y) });
  }, []);

  useEffect(() => {
    if (!ctxMenu) return undefined;
    const close = () => setCtxMenu(null);
    window.addEventListener('mousedown', close);
    window.addEventListener('scroll', close, true);
    window.addEventListener('resize', close);
    return () => {
      window.removeEventListener('mousedown', close);
      window.removeEventListener('scroll', close, true);
      window.removeEventListener('resize', close);
    };
  }, [ctxMenu]);

  const handleDictionaryLookup = useCallback(async (word: string, language: string) => {
    setCtxMenu(null);
    setDictLoading(true);
    setDictResult(null);
    try {
      const res = await fetch(`${API}/dictionary/lookup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ word, language }),
      });
      if (res.ok) {
        const result: DictionaryResult = await res.json();
        setDictResult(result);
      } else {
        setDictResult({
          word,
          source: language,
          language,
          meanings: 'Nenhuma definição encontrada.',
        });
      }
    } catch {
      setDictResult({
        word,
        source: language,
        language,
        meanings: 'Erro ao conectar com o servidor.',
      });
    } finally {
      setDictLoading(false);
    }
  }, []);

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
            <pre
              ref={lyricsRef}
              className="lyrics-text"
              style={{ fontSize: `${fontSize}rem` }}
              onContextMenu={handleLyricsContextMenu}
            >{lyrics}</pre>
          )}
        </div>
      )}

      {ctxMenu && (
        <div
          ref={ctxMenuRef}
          id="lyrics-context-menu"
          style={{ left: ctxMenu.x, top: ctxMenu.y }}
          onMouseDown={e => e.stopPropagation()}
        >
          <div className="lyrics-ctx-submenu">
            <span className="lyrics-ctx-label">Procurar no dicionário...</span>
            <div className="lyrics-ctx-submenu-items">
              <button
                type="button"
                className="lyrics-ctx-item"
                disabled={dictLoading}
                onClick={() => handleDictionaryLookup(selectedWord, 'pt')}
              >
                Português
              </button>
            </div>
          </div>
        </div>
      )}

      {dictResult && (
        <DictionaryModal result={dictResult} onClose={() => setDictResult(null)} />
      )}

      {dictLoading && (
        <div className="dict-overlay">
          <div className="dict-dialog dict-loading" onClick={e => e.stopPropagation()}>
            <div className="dict-spinner" />
            <p className="dict-loading-text">Buscando significado...</p>
          </div>
        </div>
      )}
    </section>
  );
}
