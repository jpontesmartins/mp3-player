import { useState, useCallback, useEffect, useRef } from 'react';
import { usePlayer } from '../../../app/providers/PlayerContext';
import { useLyrics } from '../lib/useLyrics';
import { Modal } from '../../../shared/ui/Modal';
import { getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';

export default function LyricsPanel() {
  const player = usePlayer();
  const {
    lyrics, loading, saving, fontSize, editing, draft, dictResult, dictLoading,
    setFontSize, setDraft, fetchCached, fetchLyrics, saveLyrics, deleteLyrics, startEdit, cancelEdit, lookupDictionary,
  } = useLyrics(player.currentFile);
  const [ctxMenu, setCtxMenu] = useState<{ x: number; y: number } | null>(null);
  const [selectedWord, setSelectedWord] = useState('');
  const lyricsRef = useRef<HTMLPreElement>(null);
  const ctxMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => { fetchCached(); }, [player.currentFile]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleLyricsContextMenu = useCallback((e: React.MouseEvent<HTMLPreElement>) => {
    const selection = window.getSelection()?.toString().trim();
    if (!selection || selection.includes(' ')) return;
    e.preventDefault();
    setSelectedWord(selection);
    setCtxMenu(getContextMenuPosition(e, ctxMenuRef.current));
  }, []);

  useContextMenuClose(!!ctxMenu, () => setCtxMenu(null));

  const canFetch = !!player.currentFile;

  return (
    <section id="lyrics-panel">
      {lyrics === null ? (
        <div className="lyrics-empty">
          <button id="fetch-lyrics-btn" onClick={fetchLyrics} disabled={!canFetch || loading}>
            {loading ? 'Buscando...' : 'Buscar letra'}
          </button>
          {!canFetch && <p className="lyrics-placeholder">Nenhuma música selecionada</p>}
        </div>
      ) : (
        <div className="lyrics-content">
          <div className="lyrics-header">
            {editing ? (
              <>
                <button id="save-lyrics-btn" onClick={saveLyrics} disabled={saving}>{saving ? 'Salvando...' : 'Salvar'}</button>
                <button id="cancel-edit-btn" onClick={cancelEdit}>Cancelar</button>
              </>
            ) : (
              <>
                <button id="fetch-lyrics-btn" onClick={fetchLyrics} disabled={loading}>{loading ? 'Buscando...' : 'Buscar letra'}</button>
                <button id="edit-lyrics-btn" onClick={startEdit}>Alterar letra</button>
                <button id="delete-lyrics-btn" onClick={deleteLyrics}>Remover letra</button>
              </>
            )}
            <div className="lyrics-font-controls">
              <button className="font-btn" onClick={() => setFontSize(s => Math.min(2.0, s + 0.1))} disabled={fontSize >= 2.0}>A+</button>
              <button className="font-btn" onClick={() => setFontSize(s => Math.max(0.7, s - 0.1))} disabled={fontSize <= 0.7}>A-</button>
            </div>
          </div>
          {editing ? (
            <textarea className="lyrics-editor" style={{ fontSize: `${fontSize}rem` }} value={draft} onChange={e => setDraft(e.target.value)} spellCheck={false} />
          ) : (
            <pre ref={lyricsRef} className="lyrics-text" style={{ fontSize: `${fontSize}rem` }} onContextMenu={handleLyricsContextMenu}>{lyrics}</pre>
          )}
        </div>
      )}

      {ctxMenu && (
        <div ref={ctxMenuRef} id="lyrics-context-menu" style={{ left: ctxMenu.x, top: ctxMenu.y }} onMouseDown={e => e.stopPropagation()}>
          <div className="lyrics-ctx-submenu">
            <span className="lyrics-ctx-label">Procurar no dicionário...</span>
            <div className="lyrics-ctx-submenu-items">
              <button type="button" className="lyrics-ctx-item" disabled={dictLoading} onClick={() => { setCtxMenu(null); lookupDictionary(selectedWord, 'pt'); }}>Português</button>
            </div>
          </div>
        </div>
      )}

      {dictResult && <Modal onClose={() => lookupDictionary('', '')} overlayClass="dict-overlay" dialogClass="dict-dialog">
        <div className="dict-header">
          <h3 className="dict-word">Significado de {dictResult.word}</h3>
          <span className="dict-source">{dictResult.source}</span>
        </div>
        <div className="dict-meanings">
          {dictResult.meanings.split('\n').map((line, i) => <p key={i} className="dict-meaning-line">{line}</p>)}
        </div>
        <button type="button" className="dict-close-btn" onClick={() => lookupDictionary('', '')}>Fechar</button>
      </Modal>}

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
