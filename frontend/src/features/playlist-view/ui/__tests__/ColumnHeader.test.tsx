import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import ColumnHeader from '../ColumnHeader';

function renderHeader(overrides = {}) {
  const defaultProps = {
    gridStyle: { gridTemplateColumns: '30% 1fr 62px' },
    headerRef: { current: null },
    startResize: () => () => {},
    dragType: null as 'artist' | 'time' | null,
    artistPct: 30,
    timePx: 62,
    ...overrides,
  };

  return render(<ColumnHeader {...defaultProps} />);
}

describe('ColumnHeader', () => {
  it('renders header with column labels', () => {
    renderHeader();
    expect(document.querySelector('#playlist-header')).toBeInTheDocument();
    expect(document.querySelector('.pl-header-artist')).toBeInTheDocument();
    expect(document.querySelector('.pl-header-title')).toBeInTheDocument();
    expect(document.querySelector('.pl-header-time')).toBeInTheDocument();
  });

  it('renders two resizers', () => {
    renderHeader();
    const resizers = document.querySelectorAll('.pl-resizer');
    expect(resizers.length).toBe(2);
  });

  it('applies grid style', () => {
    renderHeader();
    const header = document.querySelector('#playlist-header') as HTMLElement;
    expect(header.style.gridTemplateColumns).toBe('30% 1fr 62px');
  });

  it('applies dragging class when dragType is artist', () => {
    renderHeader({ dragType: 'artist' });
    const resizers = document.querySelectorAll('.pl-resizer');
    expect(resizers[0].classList.contains('dragging')).toBe(true);
    expect(resizers[1].classList.contains('dragging')).toBe(false);
  });

  it('applies dragging class when dragType is time', () => {
    renderHeader({ dragType: 'time' });
    const resizers = document.querySelectorAll('.pl-resizer');
    expect(resizers[0].classList.contains('dragging')).toBe(false);
    expect(resizers[1].classList.contains('dragging')).toBe(true);
  });
});
