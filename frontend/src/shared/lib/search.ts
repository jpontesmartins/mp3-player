import type { Id3Tags } from '../types';

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

function escapeRegex(inputString: string): string {
  return inputString.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function compareNumber(valueA: string, operator: string, valueB: string): boolean {
  const numericA = Number(valueA);
  const numericB = Number(valueB);
  if (isNaN(numericA) || isNaN(numericB)) return false;
  switch (operator) {
    case '<': return numericA < numericB;
    case '<=': return numericA <= numericB;
    case '>': return numericA > numericB;
    case '>=': return numericA >= numericB;
    case '==': return numericA === numericB;
    case '!=': return numericA !== numericB;
    default: return false;
  }
}

function compareString(valueA: string, operator: string, valueB: string): boolean {
  const lowercaseA = valueA.toLowerCase();
  const lowercaseB = valueB.toLowerCase();
  switch (operator) {
    case '==': return lowercaseA === lowercaseB;
    case '!=': return lowercaseA !== lowercaseB;
    case '<': return lowercaseA < lowercaseB;
    case '<=': return lowercaseA <= lowercaseB;
    case '>': return lowercaseA > lowercaseB;
    case '>=': return lowercaseA >= lowercaseB;
    default: return false;
  }
}

function compare(valueA: string, operator: string, valueB: string): boolean {
  if (!isNaN(Number(valueA)) && !isNaN(Number(valueB))) {
    return compareNumber(valueA, operator, valueB);
  }
  return compareString(valueA, operator, valueB);
}

function matchTerm(term: string, tags: Id3Tags): boolean {
  const trimmed = term.trim();
  if (!trimmed) return false;

  const tagMatch = trimmed.match(/^<(\w+)>\s*(==|!=|<=|>=|<|>)\s*(.+)$/);
  if (tagMatch) {
    const tagName = `<${tagMatch[1]}>`;
    const operator = tagMatch[2];
    const rawValue = tagMatch[3].trim();
    const field = TAG_MAP[tagName];
    if (!field) return false;
    const fieldValue = tags[field];
    if (!fieldValue) return false;

    const values = rawValue.split(',').map(value => value.trim()).filter(Boolean);
    if (operator === '!=' ) {
      return values.every(value => compare(fieldValue, operator, value));
    }
    return values.some(value => compare(fieldValue, operator, value));
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
  const trimmedQuery = query.trim();
  if (!trimmedQuery) return true;

  const andParts = trimmedQuery.split('&&');
  return andParts.every(part => evalGroup(part, tags));
}

export function filterPlaylist(
  files: string[],
  query: string,
  id3Cache: Map<string, Id3Tags>,
): string[] {
  const trimmedQuery = query.trim();
  if (!trimmedQuery) return files;
  return files.filter(file => {
    const tags = id3Cache.get(file);
    if (!tags) return false;
    return matchesQuery(trimmedQuery, tags);
  });
}
