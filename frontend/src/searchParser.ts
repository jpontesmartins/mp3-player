import type { Id3Tags } from './App';

const TAG_MAP: Record<string, keyof Id3Tags> = {
  '<artist>': 'artist',
  '<title>': 'title',
  '<song>': 'title',
  '<album>': 'album',
  '<year>': 'year',
  '<genre>': 'genre',
  '<track>': 'track',
  '<disc>': 'disc',
};

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function compareNum(a: string, op: string, b: string): boolean {
  const na = Number(a);
  const nb = Number(b);
  if (isNaN(na) || isNaN(nb)) return false;
  switch (op) {
    case '<': return na < nb;
    case '<=': return na <= nb;
    case '>': return na > nb;
    case '>=': return na >= nb;
    case '==': return na === nb;
    case '!=': return na !== nb;
    default: return false;
  }
}

function compareStr(a: string, op: string, b: string): boolean {
  const la = a.toLowerCase();
  const lb = b.toLowerCase();
  switch (op) {
    case '==': return la === lb;
    case '!=': return la !== lb;
    case '<': return la < lb;
    case '<=': return la <= lb;
    case '>': return la > lb;
    case '>=': return la >= lb;
    default: return false;
  }
}

function compare(a: string, op: string, b: string): boolean {
  if (!isNaN(Number(a)) && !isNaN(Number(b))) {
    return compareNum(a, op, b);
  }
  return compareStr(a, op, b);
}

function matchTerm(term: string, tags: Id3Tags): boolean {
  const trimmed = term.trim();
  if (!trimmed) return false;

  const tagMatch = trimmed.match(/^<(\w+)>\s*(==|!=|<=|>=|<|>)\s*(.+)$/);
  if (tagMatch) {
    const tagName = `<${tagMatch[1]}>`;
    const op = tagMatch[2];
    const rawValue = tagMatch[3].trim();
    const field = TAG_MAP[tagName];
    if (!field) return false;
    const fieldValue = tags[field];
    if (!fieldValue) return false;

    const values = rawValue.split(',').map(v => v.trim()).filter(Boolean);
    if (op === '!=' ) {
      return values.every(v => compare(fieldValue, op, v));
    }
    return values.some(v => compare(fieldValue, op, v));
  }

  const regex = new RegExp(escapeRegex(trimmed), 'i');
  return (
    !!tags.title && regex.test(tags.title) ||
    !!tags.artist && regex.test(tags.artist) ||
    !!tags.album && regex.test(tags.album) ||
    !!tags.genre && regex.test(tags.genre) ||
    !!tags.year && regex.test(tags.year) ||
    !!tags.track && regex.test(tags.track)
  );
}

function evalGroup(group: string, tags: Id3Tags): boolean {
  const orParts = group.split('||');
  return orParts.some(part => matchTerm(part, tags));
}

export function matchesQuery(query: string, tags: Id3Tags): boolean {
  const q = query.trim();
  if (!q) return true;

  const andParts = q.split('&&');
  return andParts.every(part => evalGroup(part, tags));
}

export function filterPlaylist(
  files: string[],
  query: string,
  id3Cache: Map<string, Id3Tags>,
): string[] {
  const q = query.trim();
  if (!q) return files;
  return files.filter(f => {
    const tags = id3Cache.get(f);
    if (!tags) return false;
    return matchesQuery(q, tags);
  });
}
