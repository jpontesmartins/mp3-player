import type { Id3Tags } from '../../../shared/types';
import { formatTime, fileName, displayName } from '../../../shared/lib/format';

interface PlaylistTooltipProps {
  file: string;
  x: number;
  y: number;
  style: { left: number; top: number } | null;
  ref: React.RefObject<HTMLDivElement | null>;
  tags: Id3Tags | undefined;
}

export default function PlaylistTooltip({ file, style, ref, tags }: PlaylistTooltipProps) {
  if (!tags) return null;

  return (
    <div ref={ref} className="id3-tooltip" style={style ?? { left: -9999, top: -9999 }}>
      <div className="id3-tooltip-title">{displayName(tags, fileName(file))}</div>
      <dl className="id3-tooltip-list">
        {tags.title && <div><dt>Música</dt><dd>{tags.title}</dd></div>}
        {tags.artist && <div><dt>Artista</dt><dd>{tags.artist}</dd></div>}
        {tags.album && <div><dt>Álbum</dt><dd>{tags.album}</dd></div>}
        {tags.year && <div><dt>Ano</dt><dd>{tags.year}</dd></div>}
        {tags.genre && <div><dt>Gênero</dt><dd>{tags.genre}</dd></div>}
        {tags.track && <div><dt>Faixa</dt><dd>{tags.track}</dd></div>}
        {tags.duration_ms && <div><dt>Duração</dt><dd>{formatTime(Number(tags.duration_ms))}</dd></div>}
        {tags.kbps && <div><dt>Bitrate</dt><dd>{tags.kbps} kbps</dd></div>}
      </dl>
    </div>
  );
}
