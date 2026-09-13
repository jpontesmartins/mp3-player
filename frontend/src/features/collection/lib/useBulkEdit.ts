import { useCallback } from 'react';
import type { Id3Tags } from '../../../shared/types';
import * as id3Api from '../../../shared/api/id3';
import { useLibrary } from '../../../app/providers/LibraryContext';

const EDITABLE_FIELDS = ['title', 'artist', 'album', 'genre', 'track', 'disc', 'year'] as const;
export type EditableField = typeof EDITABLE_FIELDS[number];

export const FIELD_LABELS: Record<EditableField, string> = {
  title: 'Música', artist: 'Artista', album: 'Álbum', genre: 'Gênero',
  track: 'Faixa', disc: 'Disco', year: 'Ano',
};

export function emptyRow(): Record<EditableField, string> {
  return { title: '', artist: '', album: '', genre: '', track: '', disc: '', year: '' };
}

export function fromTags(tags: Id3Tags | undefined): Record<EditableField, string> {
  return {
    title: tags?.title ?? '', artist: tags?.artist ?? '', album: tags?.album ?? '',
    genre: tags?.genre ?? '', track: tags?.track ?? '', disc: tags?.disc ?? '', year: tags?.year ?? '',
  };
}

export function isDirty(row: Record<EditableField, string>, tags: Id3Tags | undefined): boolean {
  return EDITABLE_FIELDS.some(k => row[k] !== (tags?.[k] ?? ''));
}

export { EDITABLE_FIELDS };

export function useBulkEdit() {
  const library = useLibrary();

  const updateTags = useCallback(async (changed: Array<{ file: string; tags: Record<string, string> }>): Promise<{ ok: number; fail: number }> => {
    let ok = 0;
    let fail = 0;
    for (const c of changed) {
      const updated = await id3Api.updateId3(c.file, c.tags);
      if (updated) {
        library.updateId3Cache(c.file, updated);
        ok++;
      } else {
        fail++;
      }
    }
    return { ok, fail };
  }, [library]);

  return { updateTags };
}
