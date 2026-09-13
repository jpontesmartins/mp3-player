import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import Playlist from '../Playlist';
import { PlayerProvider } from '../../../../app/providers/PlayerContext';
import { LibraryProvider } from '../../../../app/providers/LibraryContext';

vi.mock('@tauri-apps/plugin-opener', () => ({
  revealItemInDir: vi.fn(),
}));

vi.mock('../../../../shared/api/playlist', () => ({
  savePlaylist: vi.fn(),
}));

vi.mock('../../../../shared/api/id3', () => ({
  bulkId3: vi.fn(),
}));

function renderPlaylist() {
  return render(
    <LibraryProvider>
      <PlayerProvider>
        <Playlist />
      </PlayerProvider>
    </LibraryProvider>
  );
}

describe('Playlist', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('empty state', () => {
    it('shows empty message when no playlist files', () => {
      renderPlaylist();
      expect(screen.getByText('Nenhum arquivo .mp3 encontrado')).toBeInTheDocument();
    });

    it('renders disabled search input when empty', () => {
      renderPlaylist();
      const input = document.querySelector('.playlist-search-input') as HTMLInputElement;
      expect(input).not.toBeNull();
      expect(input.disabled).toBe(true);
    });
  });

  describe('playlist structure', () => {
    it('renders playlist section', () => {
      renderPlaylist();
      expect(document.querySelector('#playlist-section')).toBeInTheDocument();
    });

    it('renders playlist element', () => {
      renderPlaylist();
      expect(document.querySelector('#playlist')).toBeInTheDocument();
    });

    it('renders search bar', () => {
      renderPlaylist();
      expect(document.querySelector('#playlist-search-bar')).toBeInTheDocument();
      expect(document.querySelector('.playlist-search-input')).toBeInTheDocument();
    });

    it('empty state does not render column header resizers', () => {
      renderPlaylist();
      const resizers = document.querySelectorAll('.pl-resizer');
      expect(resizers.length).toBe(0);
    });
  });

  describe('search input behavior', () => {
    it('search input is focusable', () => {
      renderPlaylist();
      const input = document.querySelector('.playlist-search-input') as HTMLInputElement;
      expect(input).not.toBeNull();
      expect(input.readOnly).toBe(false);
    });
  });

  describe('save playlist', () => {
    it('save button is not visible initially', () => {
      renderPlaylist();
      expect(document.querySelector('.playlist-save-btn')).toBeNull();
    });

    it('save form is not visible initially', () => {
      renderPlaylist();
      expect(document.querySelector('.playlist-save-row')).toBeNull();
    });

    it('save message is not visible initially', () => {
      renderPlaylist();
      expect(document.querySelector('.playlist-save-msg')).toBeNull();
    });
  });

  describe('context menu', () => {
    it('context menu is not visible initially', () => {
      renderPlaylist();
      expect(document.querySelector('#song-context-menu')).toBeNull();
    });
  });

  describe('column resize', () => {
    it('resizer elements do not exist in empty state', () => {
      renderPlaylist();
      const resizers = document.querySelectorAll('.pl-resizer');
      expect(resizers.length).toBe(0);
    });
  });
});
