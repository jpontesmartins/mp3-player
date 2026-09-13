export function ContextMenuItem({ icon, label, shortcut, onClick }: {
  icon: React.ReactNode;
  label: string;
  shortcut?: string;
  onClick: () => void;
}) {
  return (
    <button type="button" className="ctx-menu-item" onClick={onClick}>
      <span className="ctx-menu-icon">{icon}</span>
      <span className="ctx-menu-label">{label}</span>
      {shortcut && <span className="ctx-menu-shortcut">{shortcut}</span>}
    </button>
  );
}

export function useContextMenuClose(
  isOpen: boolean,
  onClose: () => void,
) {
  if (!isOpen) return;
  const close = () => onClose();
  window.addEventListener('mousedown', close);
  window.addEventListener('scroll', close, true);
  window.addEventListener('resize', close);
  const cleanup = () => {
    window.removeEventListener('mousedown', close);
    window.removeEventListener('scroll', close, true);
    window.removeEventListener('resize', close);
  };
  return cleanup;
}

export function getContextMenuPosition(
  e: { clientX: number; clientY: number },
  menuElement: HTMLElement | null,
): { x: number; y: number } {
  let x = e.clientX + 4;
  let y = e.clientY + 4;
  if (menuElement) {
    const boundingRect = menuElement.getBoundingClientRect();
    if (x + boundingRect.width > window.innerWidth) x = e.clientX - boundingRect.width - 4;
    if (y + boundingRect.height > window.innerHeight) y = e.clientY - boundingRect.height - 4;
  }
  return { x: Math.max(4, x), y: Math.max(4, y) };
}
