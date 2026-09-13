import { useState, useRef, useCallback } from 'react';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';
import { getCoverUrl, downloadCover } from '../../../shared/api/cover';
import { CtxMenuItem, getContextMenuPosition, useContextMenuClose } from '../../../shared/ui/ContextMenu';

interface CoverArtProps {
  currentFile: string | null;
  showCover: boolean;
}

export default function CoverArt({ currentFile, showCover }: CoverArtProps) {
  const [coverBusy, setCoverBusy] = useState(false);
  const [coverMsg, setCoverMsg] = useState<string | null>(null);
  const [coverVersion, setCoverVersion] = useState(0);
  const [menuPos, setMenuPos] = useState<{ x: number; y: number } | null>(null);
  const coverMsgTimer = useRef<number | null>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const coverUrl = currentFile ? getCoverUrl(currentFile) : null;

  const showCoverMsg = (msg: string) => {
    setCoverMsg(msg);
    if (coverMsgTimer.current !== null) window.clearTimeout(coverMsgTimer.current);
    coverMsgTimer.current = window.setTimeout(() => setCoverMsg(null), 4000);
  };

  const handleDownloadCover = useCallback(async () => {
    if (!currentFile || coverBusy) return;
    setCoverBusy(true);
    showCoverMsg('Baixando capa...');
    const result = await downloadCover(currentFile);
    showCoverMsg(result.ok ? 'Capa baixada.' : `Erro: ${result.text}`);
    if (result.ok) setCoverVersion(v => v + 1);
    setCoverBusy(false);
  }, [currentFile, coverBusy]);

  const handleCoverContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!currentFile) return;
    setMenuPos(getContextMenuPosition(e, menuRef.current));
  }, [currentFile]);

  useContextMenuClose(!!menuPos, () => setMenuPos(null));

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
        {coverMsg && <span id="cover-status">{coverMsg}</span>}
      </div>

      {menuPos && (
        <div ref={menuRef} id="cover-context-menu" style={{ left: menuPos.x, top: menuPos.y }} onMouseDown={e => e.stopPropagation()}>
          <CtxMenuItem icon={<CloudDownloadIcon />} label="Baixar capa do álbum" onClick={() => { setMenuPos(null); handleDownloadCover(); }} />
        </div>
      )}
    </>
  );
}
