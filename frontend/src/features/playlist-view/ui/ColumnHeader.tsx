interface ColumnHeaderProps {
  gridStyle: { gridTemplateColumns: string };
  headerRef: React.RefObject<HTMLDivElement | null>;
  startResize: (type: 'artist' | 'time') => (e: React.MouseEvent) => void;
  dragType: 'artist' | 'time' | null;
  artistPct: number;
  timePx: number;
}

export default function ColumnHeader({ gridStyle, headerRef, startResize, dragType, artistPct, timePx }: ColumnHeaderProps) {
  return (
    <div id="playlist-header" ref={headerRef} style={gridStyle}>
      <span className="pl-header-artist">Artista</span>
      <span className="pl-header-title">Música</span>
      <span className="pl-header-time">Tempo</span>
      <span className={`pl-resizer ${dragType === 'artist' ? 'dragging' : ''}`} style={{ left: `calc(${artistPct}% - 4px)` }} title="Redimensionar coluna" onMouseDown={startResize('artist')} />
      <span className={`pl-resizer ${dragType === 'time' ? 'dragging' : ''}`} style={{ left: `calc(100% - ${timePx}px - 5px)` }} title="Redimensionar coluna" onMouseDown={startResize('time')} />
    </div>
  );
}
