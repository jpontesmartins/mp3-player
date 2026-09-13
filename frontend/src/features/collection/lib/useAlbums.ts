import { useMemo } from 'react';
import { useLibrary } from '../../../app/providers/LibraryContext';
import { parentDirectory } from '../../../shared/lib/format';

export interface Album {
  folder: string;
  name: string;
  files: string[];
}

function folderName(path: string): string {
  const index = Math.max(path.lastIndexOf('\\'), path.lastIndexOf('/'));
  return index < 0 ? path : path.substring(index + 1);
}

export function useAlbums() {
  const library = useLibrary();

  const albums = useMemo<Album[]>(() => {
    const map = new Map<string, { folder: string; files: string[]; albumNames: string[] }>();
    for (const file of library.libraryFiles) {
      const folder = parentDirectory(file);
      const entry = map.get(folder) ?? { folder, files: [], albumNames: [] };
      entry.files.push(file);
      const album = library.id3Cache.get(file)?.album?.trim();
      if (album) entry.albumNames.push(album);
      map.set(folder, entry);
    }
    return Array.from(map.values())
      .map(entry => {
        const counts = new Map<string, number>();
        for (const name of entry.albumNames) counts.set(name, (counts.get(name) ?? 0) + 1);
        let best: string | undefined;
        let bestCount = 0;
        for (const [name, count] of counts) {
          if (count > bestCount) { best = name; bestCount = count; }
        }
        return { folder: entry.folder, name: best ?? folderName(entry.folder), files: entry.files };
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [library.libraryFiles, library.id3Cache]);

  const artists = useMemo(() => {
    const counts = new Map<string, number>();
    for (const file of library.libraryFiles) {
      const artist = library.id3Cache.get(file)?.artist?.trim();
      if (artist) counts.set(artist, (counts.get(artist) ?? 0) + 1);
    }
    return Array.from(counts.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [library.libraryFiles, library.id3Cache]);

  return { albums, artists };
}
