import { useEffect } from 'react';

export interface DictionaryResult {
  word: string;
  source: string;
  language: string;
  meanings: string;
}

interface Props {
  result: DictionaryResult;
  onClose: () => void;
}

export default function DictionaryModal({ result, onClose }: Props) {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className="dict-overlay" onClick={onClose}>
      <div className="dict-dialog" onClick={e => e.stopPropagation()}>
        <div className="dict-header">
          <h3 className="dict-word">Significado de {result.word}</h3>
          <span className="dict-source">{result.source}</span>
        </div>
        <div className="dict-meanings">
          {result.meanings.split('\n').map((line, i) => (
            <p key={i} className="dict-meaning-line">{line}</p>
          ))}
        </div>
        <button type="button" className="dict-close-btn" onClick={onClose}>Fechar</button>
      </div>
    </div>
  );
}
