import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import SearchBar from '../../ui/SearchBar';

beforeEach(() => {
  vi.clearAllMocks();
});

function renderSearchBar(overrides = {}) {
  const defaultProps = {
    query: '',
    setQuery: vi.fn(),
    searchFocused: false,
    setSearchFocused: vi.fn(),
    searchExpanded: false,
    isFiltered: false,
    filteredCount: 0,
    totalCount: 0,
    textareaRef: { current: null },
    saveOpen: false,
    setSaveOpen: vi.fn(),
    saveName: '',
    setSaveName: vi.fn(),
    saving: false,
    saveMsg: '',
    saveInputRef: { current: null },
    handleSaveConfirm: vi.fn(),
    disabled: false,
    ...overrides,
  };

  return { ...render(<SearchBar {...defaultProps} />), props: defaultProps };
}

describe('SearchBar', () => {
  it('renders search bar', () => {
    renderSearchBar();
    expect(document.querySelector('#playlist-search-bar')).toBeInTheDocument();
    expect(document.querySelector('.playlist-search-input')).toBeInTheDocument();
  });

  it('renders search icon', () => {
    renderSearchBar();
    expect(document.querySelector('.search-icon')).toBeInTheDocument();
  });

  it('applies expanded class when searchExpanded is true', () => {
    renderSearchBar({ searchExpanded: true });
    const searchBar = document.querySelector('#playlist-search-bar');
    expect(searchBar?.classList.contains('expanded')).toBe(true);
  });

  it('does not apply expanded class when searchExpanded is false', () => {
    renderSearchBar({ searchExpanded: false });
    const searchBar = document.querySelector('#playlist-search-bar');
    expect(searchBar?.classList.contains('expanded')).toBe(false);
  });

  it('disables input when disabled prop is true', () => {
    renderSearchBar({ disabled: true });
    const input = document.querySelector('.playlist-search-input') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });

  it('does not disable input when disabled prop is false', () => {
    renderSearchBar({ disabled: false });
    const input = document.querySelector('.playlist-search-input') as HTMLInputElement;
    expect(input.disabled).toBe(false);
  });

  it('shows count when filtered', () => {
    renderSearchBar({ isFiltered: true, filteredCount: 5, totalCount: 10 });
    expect(screen.getByText('5 / 10')).toBeInTheDocument();
  });

  it('shows save button when filtered', () => {
    renderSearchBar({ isFiltered: true });
    expect(document.querySelector('.playlist-save-btn')).toBeInTheDocument();
  });

  it('does not show save button when not filtered', () => {
    renderSearchBar({ isFiltered: false });
    expect(document.querySelector('.playlist-save-btn')).toBeNull();
  });

  it('shows save form when saveOpen is true', () => {
    renderSearchBar({ saveOpen: true });
    expect(document.querySelector('.playlist-save-row')).toBeInTheDocument();
    expect(document.querySelector('.playlist-save-input')).toBeInTheDocument();
  });

  it('does not show save form when saveOpen is false', () => {
    renderSearchBar({ saveOpen: false });
    expect(document.querySelector('.playlist-save-row')).toBeNull();
  });

  it('shows save message when present', () => {
    renderSearchBar({ saveMsg: 'Playlist salva!' });
    expect(screen.getByText('Playlist salva!')).toBeInTheDocument();
  });

  it('does not show save message when empty', () => {
    renderSearchBar({ saveMsg: '' });
    expect(document.querySelector('.playlist-save-msg')).toBeNull();
  });
});
