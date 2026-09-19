import { useState, useRef, useCallback } from 'react';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import { getCoverUrl, downloadCover } from '../../../shared/api/cover';
import { ContextMenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';

interface CoverArtProps {
  currentFile: string | null;
  showCover: boolean;
}

export default function CoverArt({ currentFile, showCover }: CoverArtProps) {
  const [coverBusy, setCoverBusy] = useState(false);
  const [coverMessage, setCoverMessage] = useState<string | null>(null);
  const [coverVersion, setCoverVersion] = useState(0);
  const [menuPosition, setMenuPosition] = useState<{ x: number; y: number } | null>(null);
  const coverMessageTimer = useRef<number | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const coverUrl = currentFile ? getCoverUrl(currentFile) : null;

  const showCoverMessage = (message: string) => {
    setCoverMessage(message);
    if (coverMessageTimer.current !== null) window.clearTimeout(coverMessageTimer.current);
    coverMessageTimer.current = window.setTimeout(() => setCoverMessage(null), 4000);
  };

  const handleDownloadCover = useCallback(async () => {
    if (!currentFile || coverBusy) return;
    setCoverBusy(true);
    showCoverMessage('Baixando capa...');
    const result = await downloadCover(currentFile);
    showCoverMessage(result.ok ? 'Capa baixada.' : `Erro: ${result.text}`);
    if (result.ok) setCoverVersion(previousVersion => previousVersion + 1);
    setCoverBusy(false);
  }, [currentFile, coverBusy]);

  const handleCoverContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!currentFile) return;
    setMenuPosition(getContextMenuPosition(e, menuRef.current));
  }, [currentFile]);

  useContextMenuClose(!!menuPosition, () => setMenuPosition(null));

  if (!showCover || !currentFile) return null;

  return (
    <>
      <div id="cover-container" onContextMenu={handleCoverContextMenu} title="Clique com o botão direito para baixar a capa do álbum">
        <img
          key={`${currentFile}:${coverVersion}`}
          id="album-cover"
          src={`${coverUrl!}${coverUrl!.includes('?') ? '&' : '?'}v=${coverVersion}`}
          alt="Capa do álbum"
          onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
        />
        <span id="cover-placeholder">🎵</span>
        {coverMessage && <span id="cover-status">{coverMessage}</span>}
      </div>

      {menuPosition && (
        <div ref={menuRef} id="cover-context-menu" style={{ left: menuPosition.x, top: menuPosition.y }} onMouseDown={e => e.stopPropagation()}>
          <ContextMenuItem icon={<CloudDownloadIcon />} label="Baixar capa do álbum" onClick={() => { setMenuPosition(null); handleDownloadCover(); }} />
        </div>
      )}
    </>
  );
}
