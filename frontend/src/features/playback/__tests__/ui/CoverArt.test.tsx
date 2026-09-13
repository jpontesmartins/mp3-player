import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, act } from '@testing-library/react';
import CoverArt from '../../ui/CoverArt';

vi.mock('../../../../shared/api/cover', () => ({
  getCoverUrl: vi.fn((path: string) => `http://localhost:8111/cover?path=${path}`),
  downloadCover: vi.fn(),
}));

beforeEach(() => {
  vi.clearAllMocks();
});

describe('CoverArt', () => {
  it('renders nothing when showCover is false', () => {
    const { container } = render(<CoverArt currentFile="song.mp3" showCover={false} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders nothing when currentFile is null', () => {
    const { container } = render(<CoverArt currentFile={null} showCover={true} />);
    expect(container.innerHTML).toBe('');
  });

  it('renders cover image when showCover and currentFile are set', () => {
    render(<CoverArt currentFile="song.mp3" showCover={true} />);
    const img = document.querySelector('#album-cover') as HTMLImageElement;
    expect(img).not.toBeNull();
    expect(img.alt).toBe('Capa do álbum');
    expect(img.src).toContain('song.mp3');
  });

  it('renders placeholder', () => {
    render(<CoverArt currentFile="song.mp3" showCover={true} />);
    expect(document.querySelector('#cover-placeholder')).not.toBeNull();
  });

  it('does not show status message initially', () => {
    render(<CoverArt currentFile="song.mp3" showCover={true} />);
    expect(document.querySelector('#cover-status')).toBeNull();
  });

  it('shows context menu on right click', async () => {
    render(<CoverArt currentFile="song.mp3" showCover={true} />);
    const container = document.querySelector('#cover-container')!;

    await act(() => {
      container.dispatchEvent(new MouseEvent('contextmenu', { bubbles: true, clientX: 100, clientY: 100 }));
    });

    expect(document.querySelector('#cover-context-menu')).not.toBeNull();
  });
});
