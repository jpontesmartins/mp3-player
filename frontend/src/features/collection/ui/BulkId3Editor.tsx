import { useState, useMemo, useEffect, useCallback } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { useBulkEdit } from '../lib/useBulkEdit';
import { parentDirectory, fileName } from '../../../shared/lib/format';
import { escapeRegex } from '../../../shared/lib/format';
import * as playlistApi from '../../../shared/api/playlist';

const TAGS = ['title', 'artist', 'album', 'genre', 'track', 'disc', 'year'] as const;
type TagKey = typeof TAGS[number];
const TAG_LABELS: Record<TagKey, string> = { title: 'Música', artist: 'Artista', album: 'Álbum', genre: 'Gênero', track: 'Faixa', disc: 'Disco', year: 'Ano' };
const NARROW_TAGS: readonly TagKey[] = ['track', 'disc', 'year'];
const VAR_ALIASES: Record<string, TagKey> = { title: 'title', song: 'title', artist: 'artist', album: 'album', genre: 'genre', track: 'track', disc: 'disc', cd: 'disc', disk: 'disc', year: 'year' };

interface CompiledPattern { keys: TagKey[]; regex: RegExp; isSuccess: boolean; }
interface Row { file: string; name: string; parsed: Partial<Record<TagKey, string>> | null; merged: Record<TagKey, string>; }

function emptyMerged(): Record<TagKey, string> { return { title: '', artist: '', album: '', genre: '', track: '', disc: '', year: '' }; }

function compilePattern(pattern: string): CompiledPattern {
  const trimmed = pattern.trim();
  if (!trimmed) return { keys: [], regex: /^$/, isSuccess: false };
  const tokens = trimmed.split(/(<[a-zA-Z]+>)/g).filter(token => token.length > 0);
  const placeholders = tokens.filter(token => /^<[a-zA-Z]+>$/.test(token));
  const keys: TagKey[] = [];
  let placeholderIndex = 0;
  let regexPattern = '^';
  for (const token of tokens) {
    const match = /^<([a-zA-Z]+)>$/.exec(token);
    if (match) {
      const key = VAR_ALIASES[match[1]];
      if (!key) return { keys: [], regex: /^$/, isSuccess: false };
      keys.push(key);
      regexPattern += placeholderIndex === placeholders.length - 1 ? '(.+)' : '(.+?)';
      placeholderIndex++;
    } else { regexPattern += escapeRegex(token); }
  }
  regexPattern += '$';
  return { keys, regex: new RegExp(regexPattern), isSuccess: keys.length > 0 };
}

function parseFilename(name: string, compiled: CompiledPattern): Partial<Record<TagKey, string>> | null {
  const stem = name.replace(/\.[^.]*$/, '').trim();
  const match = compiled.regex.exec(stem);
  if (!match) return null;
  const output: Partial<Record<TagKey, string>> = {};
  for (let i = 0; i < compiled.keys.length; i++) output[compiled.keys[i]] = (match[i + 1] ?? '').trim();
  return output;
}

interface Props { initialPath?: string; }

export default function BulkId3Editor({ initialPath }: Props) {
  const library = useLibrary();
  const { updateTags } = useBulkEdit();
  const defaultFolder = useMemo(() => {
    if (initialPath) return initialPath;
    const firstFile = library.libraryFiles[0];
    return firstFile ? parentDirectory(firstFile) : '';
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
    const parsed = compiled.isSuccess ? parseFilename(fileName(file), compiled) : null;
    const merged = emptyMerged();
    for (const tag of TAGS) merged[tag] = fixed[tag].trim() !== '' ? fixed[tag].trim() : (parsed?.[tag] ?? '');
    return { file, name: fileName(file), parsed, merged };
  }), [files, compiled, fixed]);

  const loadFolder = useCallback(async (path: string) => {
    setLoadingFolder(true); setFolderError(''); setMessage(''); setFiles([]);
    const data = await playlistApi.loadFolder(path);
    if (data) setFiles(data.filter(filePath => /\.mp3$/i.test(filePath)));
    else setFolderError('Não foi possível ler a pasta');
    setLoadingFolder(false);
  }, []);

  useEffect(() => { if (folder) loadFolder(folder); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleApply = useCallback(async () => {
    const targets = rows.filter(row => Object.values(row.merged).some(value => value.trim() !== ''));
    if (targets.length === 0) { setMessage('Nada para aplicar'); return; }
    setSaving(true); setMessage('');
    const changed = targets.map(target => ({
      file: target.file,
      tags: Object.fromEntries(Object.entries(target.merged).filter(([, value]) => (value as string).trim() !== '')),
    }));
    const { successCount, failureCount } = await updateTags(changed);
    setMessage(`${successCount} atualizado(s)${failureCount ? `, ${failureCount} com erro` : ''}`);
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
          {['title', 'artist', 'album', 'genre'].map(tag => (
            <div className="bulk-field" key={tag}>
              <span className="bulk-field-label">{TAG_LABELS[tag as TagKey]}</span>
              <input className="bulk-input" value={fixed[tag as TagKey]} onChange={e => setFixed(previous => ({ ...previous, [tag]: e.target.value }))} />
            </div>
          ))}
        </div>
        <div className="bulk-fields bulk-fields-narrow">
          {NARROW_TAGS.map(tag => (
            <div className="bulk-field bulk-field-narrow" key={tag}>
              <span className="bulk-field-label">{TAG_LABELS[tag]}</span>
              <input className="bulk-input" placeholder={tag === 'track' ? 'ex: 03' : tag === 'disc' ? 'ex: 1/2' : ''} value={fixed[tag]} onChange={e => setFixed(previous => ({ ...previous, [tag]: e.target.value }))} />
            </div>
          ))}
        </div>
      </div>

      {(rows.length > 0 || true) && (
        <>
          <div className="bulk-table-wrap">
            <table className="bulk-table">
              <thead><tr><th>Arquivo</th>{TAGS.map(tag => <th key={tag}>{TAG_LABELS[tag]}</th>)}</tr></thead>
              <tbody>
                {rows.map(row => (
                  <tr key={row.file}>
                    <td className="bulk-file">{row.name}{row.parsed === null && <span className="bulk-fail"> · não corresponde</span>}</td>
                    {TAGS.map(tag => <td key={tag} className={row.parsed && row.parsed[tag] ? 'bulk-fromname' : ''}>{row.merged[tag]}</td>)}
                  </tr>
                ))}
                {Array.from({ length: Math.max(0, 5 - rows.length) }, (_, index) => (
                  <tr key={`empty-${index}`} className="bulk-empty-row"><td className="bulk-file"></td>{TAGS.map(tag => <td key={tag}></td>)}</tr>
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
