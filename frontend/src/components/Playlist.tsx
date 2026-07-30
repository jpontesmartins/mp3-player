import type { Id3Tags } from '../App';

function formatTime(ms: number): string {
  if (!ms || ms <= 0) return '00:00:00';
  const total = Math.floor(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

interface Props {
  files: string[];
  currentFile: string | null;
  id3Cache: Map<string, Id3Tags>;
  onSelect: (file: string) => void;
}

function fileName(path: string): string {
  return path.split('\\').pop()!.split('/').pop()!;
}

function displayName(tags: Id3Tags | undefined, file: string): string {
  if (tags) {
    const artist = tags.artist;
    const title = tags.title;
    if (artist && title) return `${artist} - ${title}`;
    if (title) return title;
  }
  return fileName(file);
}

function totalDuration(files: string[], cache: Map<string, Id3Tags>): number {
  let total = 0;
  for (const f of files) {
    const d = cache.get(f)?.duration_ms;
    if (d) total += Number(d);
  }
  return total;
}

export default function Playlist({ files, currentFile, id3Cache, onSelect }: Props) {
  if (files.length === 0) {
    return (
      <section id="playlist-section">
        <ul id="playlist">
          <li style={{ color: '#666' }}>Nenhum arquivo .mp3 encontrado</li>
        </ul>
      </section>
    );
  }

  const total = totalDuration(files, id3Cache);

  return (
    <section id="playlist-section">
      <ul id="playlist">
        {files.map((file) => {
          const tags = id3Cache.get(file);
          const name = displayName(tags, file);
          const dur = tags?.duration_ms ? Number(tags.duration_ms) : 0;
          const active = file === currentFile;
          return (
            <li
              key={file}
              className={active ? 'active' : ''}
              onClick={() => onSelect(file)}
            >
              <span className="pl-name">{name}</span>
              {dur > 0 && <span className="pl-duration">{formatTime(dur)}</span>}
            </li>
          );
        })}
      </ul>
      <div id="playlist-footer">
        <span>{files.length} {files.length === 1 ? 'música' : 'músicas'}</span>
        {total > 0 && <span>{formatTime(total)}</span>}
      </div>
    </section>
  );
}
