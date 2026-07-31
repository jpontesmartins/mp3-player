import { useEffect } from 'react';
import appIcon from '../../icone-v2.png';

interface Props {
  onClose: () => void;
}

export default function InfoModal({ onClose }: Props) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className="info-overlay" onClick={onClose}>
      <div className="info-dialog" onClick={e => e.stopPropagation()}>
        <img src={appIcon} width={128} height={128} alt="Logo" />
        <p className="info-title">Gerenciador de biblioteca de músicas.</p>
        <p className="info-subtitle">Desenvolvido por ovelha-eletrica.</p>
        <button id="info-close-btn" onClick={onClose}>Fechar</button>
      </div>
    </div>
  );
}
