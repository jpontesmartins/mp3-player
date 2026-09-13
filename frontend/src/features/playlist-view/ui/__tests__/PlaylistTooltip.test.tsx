import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import PlaylistTooltip from '../PlaylistTooltip';
import type { Id3Tags } from '../../../../shared/types';

const tags: Id3Tags = {
  title: 'Bohemian Rhapsody',
  artist: 'Queen',
  album: 'A Night at the Opera',
  year: '1975',
  genre: 'Rock',
  track: '12',
  duration_ms: '354000',
  kbps: '320',
};

function renderTooltip(overrides = {}) {
  const defaultProps = {
    file: '/music/bohemian.mp3',
    x: 100,
    y: 200,
    style: { left: 114, top: 214 },
    ref: { current: null },
    tags,
    ...overrides,
  };

  return render(<PlaylistTooltip {...defaultProps} />);
}

describe('PlaylistTooltip', () => {
  it('renders nothing when tags is undefined', () => {
    const { container } = renderTooltip({ tags: undefined });
    expect(container.innerHTML).toBe('');
  });

  it('renders tooltip with title', () => {
    renderTooltip();
    expect(document.querySelector('.id3-tooltip')).toBeInTheDocument();
    expect(document.querySelector('.id3-tooltip-title')).toHaveTextContent('Queen - Bohemian Rhapsody');
  });

  it('renders all tag fields', () => {
    renderTooltip();
    expect(screen.getByText('Bohemian Rhapsody')).toBeInTheDocument();
    expect(screen.getByText('Queen')).toBeInTheDocument();
    expect(screen.getByText('A Night at the Opera')).toBeInTheDocument();
    expect(screen.getByText('1975')).toBeInTheDocument();
    expect(screen.getByText('Rock')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('00:05:54')).toBeInTheDocument();
    expect(screen.getByText('320 kbps')).toBeInTheDocument();
  });

  it('applies style', () => {
    renderTooltip();
    const tooltip = document.querySelector('.id3-tooltip') as HTMLElement;
    expect(tooltip.style.left).toBe('114px');
    expect(tooltip.style.top).toBe('214px');
  });

  it('falls back to hidden position when style is null', () => {
    renderTooltip({ style: null });
    const tooltip = document.querySelector('.id3-tooltip') as HTMLElement;
    expect(tooltip.style.left).toBe('-9999px');
    expect(tooltip.style.top).toBe('-9999px');
  });
});

import { screen } from '@testing-library/react';
