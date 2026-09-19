import SearchIcon from '@mui/icons-material/Search';
import SaveIcon from '@mui/icons-material/Save';

interface SearchBarProps {
  query: string;
  setQuery: (value: string) => void;
  searchFocused: boolean;
  setSearchFocused: (value: boolean) => void;
  searchExpanded: boolean;
  isFiltered: boolean;
  filteredCount: number;
  totalCount: number;
  textareaRef: React.RefObject<HTMLInputElement | null>;
  saveOpen: boolean;
  setSaveOpen: (value: boolean) => void;
  saveName: string;
  setSaveName: (value: string) => void;
  saving: boolean;
  saveMessage: string;
  saveInputRef: React.RefObject<HTMLInputElement | null>;
  handleSaveConfirm: () => void;
  disabled?: boolean;
}

export default function SearchBar({
  query, setQuery, setSearchFocused, searchExpanded,
  isFiltered, filteredCount, totalCount, textareaRef,
  saveOpen, setSaveOpen, saveName, setSaveName, saving, saveMessage, saveInputRef, handleSaveConfirm, disabled,
}: SearchBarProps) {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      setQuery('');
      textareaRef.current?.blur();
    }
  };

  const handleSaveKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSaveConfirm();
    if (e.key === 'Escape') setSaveOpen(false);
  };

  return (
    <>
      <div id="playlist-search-bar" className={searchExpanded ? 'expanded' : ''}>
        <span className="search-icon"><SearchIcon /></span>
        <div className="playlist-search-field">
          <input ref={textareaRef} className="playlist-search-input" placeholder="" type="text" value={query} onChange={e => setQuery(e.target.value)} onFocus={() => setSearchFocused(true)} onBlur={() => setSearchFocused(false)} onKeyDown={handleKeyDown} disabled={disabled} />
          {isFiltered && <span className="playlist-search-count">{filteredCount} / {totalCount}</span>}
        </div>
        {isFiltered && (
          <button className="playlist-save-btn" title="Salvar resultado como playlist" onClick={() => { setSaveOpen(true); setSaveName(''); }}>
            <SaveIcon />
          </button>
        )}
      </div>

      {saveOpen && (
        <div className="playlist-save-row">
          <input ref={saveInputRef} className="playlist-save-input" placeholder="Nome da playlist" value={saveName} onChange={e => setSaveName(e.target.value)} onKeyDown={handleSaveKeyDown} />
          <button className="pmanager-btn primary" onClick={handleSaveConfirm} disabled={saving || !saveName.trim()}>{saving ? '...' : 'Salvar'}</button>
          <button className="pmanager-btn" onClick={() => setSaveOpen(false)}>Cancelar</button>
        </div>
      )}

      {saveMessage && <div className="playlist-save-msg">{saveMessage}</div>}
    </>
  );
}
