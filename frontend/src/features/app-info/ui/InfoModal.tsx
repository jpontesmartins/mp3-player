import appIcon from '../../../../icone-v2.png';
import { Modal } from '../../../shared/ui/Modal';
import { useApp } from '../../../app/providers/AppContext';

export default function InfoModal() {
  const app = useApp();
  return (
    <Modal onClose={() => app.setShowInfo(false)} overlayClass="info-overlay" dialogClass="info-dialog">
      <img src={appIcon} width={128} height={128} alt="Logo" />
      <p className="info-title">Gerenciador de biblioteca de músicas.</p>
      <p className="info-subtitle">Desenvolvido por ovelha-eletrica.</p>
      <button id="info-close-btn" onClick={() => app.setShowInfo(false)}>Fechar</button>
    </Modal>
  );
}
