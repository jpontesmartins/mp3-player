import { useState, useCallback } from 'react';
import * as lyricsApi from '../../../shared/api/lyrics';
import * as dictionaryApi from '../../../shared/api/dictionary';
import type { DictionaryResult } from '../../../shared/types';

const MIN_SIZE = 0.7;
const MAX_SIZE = 2.0;
const FONT_SIZE_KEY = 'lyrics_font_size';

function loadFontSize(): number {
  const saved = localStorage.getItem(FONT_SIZE_KEY);
  if (saved) {
    const n = parseFloat(saved);
    if (!isNaN(n) && n >= MIN_SIZE && n <= MAX_SIZE) return n;
  }
  return 0.9;
}

export function useLyrics(currentFile: string | null) {
  const [lyrics, setLyrics] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fontSize, setFontSize] = useState(loadFontSize);
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState('');
  const [dictResult, setDictResult] = useState<DictionaryResult | null>(null);
  const [dictLoading, setDictLoading] = useState(false);

  const fetchCached = useCallback(async () => {
    if (!currentFile) return;
    setLyrics(null);
    setEditing(false);
    const result = await lyricsApi.getCached(currentFile);
    if (result.ok) setLyrics(result.text);
  }, [currentFile]);

  const fetchLyrics = useCallback(async () => {
    if (!currentFile) return;
    setLoading(true);
    setLyrics(null);
    const result = await lyricsApi.get(currentFile);
    setLyrics(result.ok ? result.text : `Erro: ${result.text}`);
    setLoading(false);
  }, [currentFile]);

  const saveLyrics = useCallback(async () => {
    if (!currentFile) return;
    setSaving(true);
    const ok = await lyricsApi.save(currentFile, draft);
    if (ok) {
      setLyrics(draft);
      setEditing(false);
    }
    setSaving(false);
  }, [currentFile, draft]);

  const deleteLyrics = useCallback(async () => {
    if (!currentFile) return;
    if (!confirm('Remover letra salva?')) return;
    const result = await lyricsApi.remove(currentFile);
    if (result.ok) setLyrics(null);
  }, [currentFile]);

  const startEdit = useCallback(() => {
    setDraft(lyrics ?? '');
    setEditing(true);
  }, [lyrics]);

  const cancelEdit = useCallback(() => setEditing(false), []);

  const lookupDictionary = useCallback(async (word: string, language: string) => {
    setDictLoading(true);
    setDictResult(null);
    const result = await dictionaryApi.lookup(word, language);
    setDictResult(result ?? {
      word, source: language, language,
      meanings: 'Nenhuma definição encontrada.',
    });
    setDictLoading(false);
  }, []);

  return {
    lyrics, loading, saving, fontSize, editing, draft, dictResult, dictLoading,
    setFontSize, setDraft,
    fetchCached, fetchLyrics, saveLyrics, deleteLyrics, startEdit, cancelEdit, lookupDictionary,
  };
}
