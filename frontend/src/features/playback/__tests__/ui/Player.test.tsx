import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import Player from '../../ui/Player';
import { PlayerProvider } from '../../../../app/providers/PlayerContext';
import { LibraryProvider } from '../../../../app/providers/LibraryContext';

vi.mock('../../../../shared/api/playback', () => ({
  play: vi.fn(),
  pause: vi.fn(),
  resume: vi.fn(),
  stop: vi.fn(),
  seek: vi.fn(),
}));

vi.mock('../../../../shared/api/cover', () => ({
  getCoverUrl: vi.fn(() => 'http://localhost:8111/cover?path=test'),
  downloadCover: vi.fn(),
}));

function renderPlayer() {
  return render(
    <LibraryProvider>
      <PlayerProvider>
        <Player />
      </PlayerProvider>
    </LibraryProvider>
  );
}

describe('Player', () => {
  it('shows stopped message when no file is playing', () => {
    renderPlayer();
    expect(screen.getByText('Nenhuma música tocando')).toBeInTheDocument();
  });

  it('disables controls when no file is loaded', () => {
    renderPlayer();
    const prevBtn = document.querySelector('#prev-btn') as HTMLButtonElement;
    const playBtn = document.querySelector('#play-pause-btn') as HTMLButtonElement;
    const stopBtn = document.querySelector('#stop-btn') as HTMLButtonElement;
    const nextBtn = document.querySelector('#next-btn') as HTMLButtonElement;
    expect(prevBtn).not.toBeNull();
    expect(prevBtn.disabled).toBe(true);
    expect(playBtn.disabled).toBe(true);
    expect(stopBtn.disabled).toBe(true);
    expect(nextBtn.disabled).toBe(true);
  });

  it('shows 00:00:00 / --:--:-- when stopped', () => {
    renderPlayer();
    expect(screen.getByText('00:00:00 / --:--:--')).toBeInTheDocument();
  });

  it('renders progress bar', () => {
    renderPlayer();
    expect(document.querySelector('#progress-bar')).toBeInTheDocument();
    expect(document.querySelector('#progress-fill')).toBeInTheDocument();
  });

  it('renders player controls section', () => {
    renderPlayer();
    expect(document.querySelector('#player-controls')).toBeInTheDocument();
    expect(document.querySelector('#play-pause-btn')).toBeInTheDocument();
    expect(document.querySelector('#stop-btn')).toBeInTheDocument();
    expect(document.querySelector('#prev-btn')).toBeInTheDocument();
    expect(document.querySelector('#next-btn')).toBeInTheDocument();
  });
});
