import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import NewPlaylistModal from '../../ui/NewPlaylistModal';
import { LibraryProvider } from '../../../../app/providers/LibraryContext';
import { listPlaylists, savePlaylist } from '../../../../shared/api/playlist';

vi.mock('../../../../shared/api/playlist', () => ({
  listPlaylists: vi.fn(),
  savePlaylist: vi.fn(),
}));

function renderModal(onClose = vi.fn(), onCreated = vi.fn()) {
  render(
    <LibraryProvider>
      <NewPlaylistModal file="C:\music\a.mp3" onClose={onClose} onCreated={onCreated} />
    </LibraryProvider>
  );
  return { onClose, onCreated };
}

describe('NewPlaylistModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(listPlaylists).mockResolvedValue([]);
    vi.mocked(savePlaylist).mockResolvedValue(true);
  });

  it('renders title, focused input and buttons; Salvar disabled when empty', () => {
    renderModal();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Nova playlist')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Nome da playlist')).toHaveFocus();
    expect(screen.getByText('Cancelar')).toBeInTheDocument();
    expect(screen.getByText('Salvar')).toBeDisabled();
  });

  it('saves on Enter', async () => {
    const { onCreated, onClose } = renderModal();
    const input = screen.getByPlaceholderText('Nome da playlist');
    fireEvent.change(input, { target: { value: 'Viagem' } });
    fireEvent.keyDown(input, { key: 'Enter' });
    await waitFor(() => expect(savePlaylist).toHaveBeenCalledWith('Viagem', ['C:\\music\\a.mp3']));
    await waitFor(() => expect(onClose).toHaveBeenCalled());
    expect(onCreated).toHaveBeenCalledWith('Viagem');
  });

  it('closes via Cancelar, Esc and overlay without saving', () => {
    const { onClose } = renderModal();
    fireEvent.click(screen.getByText('Cancelar'));
    fireEvent.keyDown(window, { key: 'Escape' });
    fireEvent.click(document.querySelector('.confirm-overlay')!);
    expect(onClose).toHaveBeenCalledTimes(3);
    expect(savePlaylist).not.toHaveBeenCalled();
  });

  it('shows error with role alert', async () => {
    vi.mocked(listPlaylists).mockResolvedValue(['Rock']);
    renderModal();
    fireEvent.change(screen.getByPlaceholderText('Nome da playlist'), { target: { value: 'rock' } });
    fireEvent.click(screen.getByText('Salvar'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Já existe uma playlist com esse nome');
  });
});
