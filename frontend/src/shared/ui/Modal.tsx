import { useEffect } from 'react';

interface Props {
  onClose: () => void;
  overlayClass: string;
  dialogClass: string;
  children: React.ReactNode;
}

export function Modal({ onClose, overlayClass, dialogClass, children }: Props) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className={overlayClass} onClick={onClose}>
      <div className={dialogClass} onClick={e => e.stopPropagation()}>
        {children}
      </div>
    </div>
  );
}
