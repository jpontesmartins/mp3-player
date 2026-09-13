import { useState, useMemo, useEffect, useCallback } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { useBulkEdit } from '../lib/useBulkEdit';
import { parentDir, fileName } from '../../../shared/lib/format';
import { escapeRegex } from '../../../shared/lib/format';
import * as playlistApi from '../../../shared/api/playlist';

const TAGS = ['title', 'artist', 'album', 'genre', 'track', 'disc', 'year'] as const;
type TagKey = typeof TAGS[number];
const TAG_LABELS: Record<TagKey, string> = { title: 'Música', artist: 'Artista', album: 'Álbum', genre: 'Gênero', track: 'Faixa', disc: 'Disco', year: 'Ano' };
const NARROW_TAGS: readonly TagKey[] = ['track', 'disc', 'year'];
const VAR_ALIASES: Record<string, TagKey> = { title: 'title', song: 'title', artist: 'artist', album: 'album', genre: 'genre', track: 'track', disc: 'disc', cd: 'disc', disk: 'disc', year: 'year' };

interface CompiledPattern { keys: TagKey[]; regex: RegExp; ok: boolean; }
interface Row { file: string; name: string; parsed: Partial<Record<TagKey, string>> | null; merged: Record<TagKey, string>; }

function emptyMerged(): Record<TagKey, string> { return { title: '', artist: '', album: '', genre: '', track: '', disc: '', year: '' }; }

function compilePattern(pattern: string): CompiledPattern {
  const trimmed = pattern.trim();
  if (!trimmed) return { keys: [], regex: /^$/, ok: false };
  const tokens = trimmed.split(/(<[a-zA-Z]+>)/g).filter(t => t.length > 0);
  const placeholders = tokens.filter(t => /^<[a-zA-Z]+>$/.test(t));
  const keys: TagKey[] = [];
  let phIdx = 0;
  let rx = '^';
  for (const tok of tokens) {
    const m = /^<([a-zA-Z]+)>$/.exec(tok);
    if (m) {
      const key = VAR_ALIASES[m[1]];
      if (!key) return { keys: [], regex: /^$/, ok: false };
      keys.push(key);
      rx += phIdx === placeholders.length - 1 ? '(.+)' : '(.+?)';
      phIdx++;
    } else { rx += escapeRegex(tok); }
  }
  rx += '$';
  return { keys, regex: new RegExp(rx), ok: keys.length > 0 };
}

function parseFilename(name: string, compiled: CompiledPattern): Partial<Record<TagKey, string>> | null {
  const stem = name.replace(/\.[^.]*$/, '').trim();
  const m = compiled.regex.exec(stem);
  if (!m) return null;
  const out: Partial<Record<TagKey, string>> = {};
  for (let i = 0; i < compiled.keys.length; i++) out[compiled.keys[i]] = (m[i + 1] ?? '').trim();
  return out;
}

interface Props { initialPath?: string; }

export default function BulkId3Editor({ initialPath }: Props) {
  const library = useLibrary();
  const { updateTags } = useBulkEdit();
  const defaultFolder = useMemo(() => {
    if (initialPath) return initialPath;
    const f0 = library.libraryFiles[0];
    return f0 ? parentDir(f0) : '';
  }, [library.libraryFiles, initialPath]);

  const [folder, setFolder] = useState(defaultFolder);
  const [files, setFiles] = useState<string[]>([]);
  const [loadingFolder, setLoadingFolder] = useState(false);
  const [folderError, setFolderError] = useState('');
  const [pattern, setPattern] = useState('');
  const [fixed, setFixed] = useState<Record<TagKey, string>>(emptyMerged());
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const compiled = useMemo(() => compilePattern(pattern), [pattern]);

  const rows = useMemo<Row[]>(() => files.map(file => {
    const parsed = compiled.ok ? parseFilename(fileName(file), compiled) : null;
    const merged = emptyMerged();
    for (const k of TAGS) merged[k] = fixed[k].trim() !== '' ? fixed[k].trim() : (parsed?.[k] ?? '');
    return { file, name: fileName(file), parsed, merged };
  }), [files, compiled, fixed]);

  const loadFolder = useCallback(async (path: string) => {
    setLoadingFolder(true); setFolderError(''); setMessage(''); setFiles([]);
    const data = await playlistApi.loadFolder(path);
    if (data) setFiles(data.filter(f => /\.mp3$/i.test(f)));
    else setFolderError('Não foi possível ler a pasta');
    setLoadingFolder(false);
  }, []);

  useEffect(() => { if (folder) loadFolder(folder); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleApply = useCallback(async () => {
    const targets = rows.filter(r => Object.values(r.merged).some(v => v.trim() !== ''));
    if (targets.length === 0) { setMessage('Nada para aplicar'); return; }
    setSaving(true); setMessage('');
    const changed = targets.map(t => ({
      file: t.file,
      tags: Object.fromEntries(Object.entries(t.merged).filter(([, v]) => (v as string).trim() !== '')),
    }));
    const { ok, fail } = await updateTags(changed);
    setMessage(`${ok} atualizado(s)${fail ? `, ${fail} com erro` : ''}`);
    setSaving(false);
  }, [rows, updateTags]);

  const matched = rows.filter(r => r.parsed !== null).length;

  return (
    <div className="bulk">
      <div className="bulk-row">
        <input className="bulk-input" placeholder="C:\caminho" value={folder} onChange={e => setFolder(e.target.value)} />
        <button className="pmanager-btn primary" onClick={() => loadFolder(folder)} disabled={loadingFolder || !folder.trim()}>{loadingFolder ? 'Carregando...' : 'Carregar pasta'}</button>
      </div>
      {folderError && <div className="pmanager-error">{folderError}</div>}
      <div className="bulk-count">{files.length > 0 ? `${rows.length} arquivo(s) .mp3 · ${matched} casam com o padrão` : 'Nenhum arquivo carregado'}</div>

      <div className="bulk-group">
        <label className="settings-label">Padrão do nome do arquivo</label>
        <input className="bulk-input" placeholder="Ex: <artist> - <song>  ou  <track> - <song>" value={pattern} onChange={e => setPattern(e.target.value)} />
        <div className="bulk-hint">
          Tags: <code>&lt;title&gt;</code> (ou <code>&lt;song&gt;</code>), <code>&lt;artist&gt;</code>, <code>&lt;album&gt;</code>, <code>&lt;genre&gt;</code>, <code>&lt;track&gt;</code>, <code>&lt;disc&gt;</code> (ou <code>&lt;cd&gt;</code>), <code>&lt;year&gt;</code>
        </div>
      </div>

      <div className="bulk-group">
        <label className="settings-label">Valores fixos (aplicados a todos os arquivos)</label>
        <div className="bulk-fields">
          {['title', 'artist', 'album', 'genre'].map(k => (
            <div className="bulk-field" key={k}>
              <span className="bulk-field-label">{TAG_LABELS[k as TagKey]}</span>
              <input className="bulk-input" value={fixed[k as TagKey]} onChange={e => setFixed(prev => ({ ...prev, [k]: e.target.value }))} />
            </div>
          ))}
        </div>
        <div className="bulk-fields bulk-fields-narrow">
          {NARROW_TAGS.map(k => (
            <div className="bulk-field bulk-field-narrow" key={k}>
              <span className="bulk-field-label">{TAG_LABELS[k]}</span>
              <input className="bulk-input" placeholder={k === 'track' ? 'ex: 03' : k === 'disc' ? 'ex: 1/2' : ''} value={fixed[k]} onChange={e => setFixed(prev => ({ ...prev, [k]: e.target.value }))} />
            </div>
          ))}
        </div>
      </div>

      {(rows.length > 0 || true) && (
        <>
          <div className="bulk-table-wrap">
            <table className="bulk-table">
              <thead><tr><th>Arquivo</th>{TAGS.map(k => <th key={k}>{TAG_LABELS[k]}</th>)}</tr></thead>
              <tbody>
                {rows.map(r => (
                  <tr key={r.file}>
                    <td className="bulk-file">{r.name}{r.parsed === null && <span className="bulk-fail"> · não corresponde</span>}</td>
                    {TAGS.map(k => <td key={k} className={r.parsed && r.parsed[k] ? 'bulk-fromname' : ''}>{r.merged[k]}</td>)}
                  </tr>
                ))}
                {Array.from({ length: Math.max(0, 5 - rows.length) }, (_, i) => (
                  <tr key={`empty-${i}`} className="bulk-empty-row"><td className="bulk-file"></td>{TAGS.map(k => <td key={k}></td>)}</tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="bulk-actions">
            {message && <span className="pmanager-msg">{message}</span>}
            <button className="pmanager-btn primary" onClick={handleApply} disabled={saving || rows.length === 0}>{saving ? 'Aplicando...' : 'Aplicar ID3 em massa'}</button>
          </div>
        </>
      )}
    </div>
  );
}
