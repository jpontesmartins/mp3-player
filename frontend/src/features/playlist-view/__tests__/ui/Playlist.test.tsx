import { describe, it, expect, vi, beforeEach } from 'vitest';
import { useEffect } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import Playlist from '../../ui/Playlist';
import { PlayerProvider } from '../../../../app/providers/PlayerContext';
import { LibraryProvider, useLibrary } from '../../../../app/providers/LibraryContext';
import { listPlaylists, loadVirtual, savePlaylist } from '../../../../shared/api/playlist';

vi.mock('@tauri-apps/plugin-opener', () => ({
  revealItemInDir: vi.fn(),
}));

vi.mock('../../../../shared/api/playlist', () => ({
  listPlaylists: vi.fn(),
  loadVirtual: vi.fn(),
  savePlaylist: vi.fn(),
}));

vi.mock('../../../../shared/api/id3', () => ({
  bulkId3: vi.fn(),
}));

function SeedFiles({ files }: { files: string[] }) {
  const library = useLibrary();
  useEffect(() => {
    library.setPlaylistFiles(files);
    library.setLibraryFiles(files);
  }, [library, files]);
  return <Playlist />;
}

function renderPlaylist() {
  return render(
    <LibraryProvider>
      <PlayerProvider>
        <Playlist />
      </PlayerProvider>
    </LibraryProvider>
  );
}

function renderPlaylistWithFiles(files: string[]) {
  return render(
    <LibraryProvider>
      <PlayerProvider>
        <SeedFiles files={files} />
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

    it('lists existing playlists as submenu items on right click', async () => {
      vi.mocked(listPlaylists).mockResolvedValue(['Rock', 'Pop']);
      renderPlaylistWithFiles(['C:\\music\\a.mp3']);
      fireEvent.contextMenu(screen.getByText('a.mp3'));
      expect(await screen.findByText('Adicionar à playlist')).toBeInTheDocument();
      expect(screen.getByText('Rock')).toBeInTheDocument();
      expect(screen.getByText('Pop')).toBeInTheDocument();
    });

    it('appends the song to the chosen playlist', async () => {
      vi.mocked(listPlaylists).mockResolvedValue(['Rock']);
      vi.mocked(loadVirtual).mockResolvedValue(['C:\\music\\b.mp3']);
      vi.mocked(savePlaylist).mockResolvedValue(true);
      renderPlaylistWithFiles(['C:\\music\\a.mp3']);
      fireEvent.contextMenu(screen.getByText('a.mp3'));
      fireEvent.click(await screen.findByText('Rock'));
      await waitFor(() => expect(savePlaylist).toHaveBeenCalledWith('Rock', ['C:\\music\\b.mp3', 'C:\\music\\a.mp3']));
    });

    it('does not duplicate a song already in the playlist', async () => {
      vi.mocked(listPlaylists).mockResolvedValue(['Rock']);
      vi.mocked(loadVirtual).mockResolvedValue(['C:\\music\\a.mp3']);
      renderPlaylistWithFiles(['C:\\music\\a.mp3']);
      fireEvent.contextMenu(screen.getByText('a.mp3'));
      fireEvent.click(await screen.findByText('Rock'));
      await waitFor(() => expect(screen.getByText('"a.mp3" já está em "Rock"')).toBeInTheDocument());
      expect(savePlaylist).not.toHaveBeenCalled();
    });

    it('shows a hint when no playlist exists', async () => {
      vi.mocked(listPlaylists).mockResolvedValue([]);
      renderPlaylistWithFiles(['C:\\music\\a.mp3']);
      fireEvent.contextMenu(screen.getByText('a.mp3'));
      expect(await screen.findByText('Nenhuma playlist criada')).toBeInTheDocument();
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
