# Especificação Técnica — ofy-005 — Busca avançada na playlist

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Motor puro em `shared/lib/search.ts` (`matchesQuery`, `filterPlaylist`) usado por `usePlaylistSearch` (lista principal) e `PlaylistManager` (editor de playlists). Salvar resultado usa `POST /playlist`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `shared/lib/search.ts`; `features/playlist-view/lib/usePlaylistSearch.ts`; `features/playlist-view/ui/SearchBar.tsx`; `features/collection/ui/PlaylistManager.tsx` (reuso).
- Fluxo: `query` → `useMemo(filterPlaylist(playlistFiles, query, id3Cache))` → `filteredFiles` → renderização; salvar → `playlistApi.savePlaylist(name, filteredFiles)` → `library.refreshPlaylists()`.

## Design de Implementação

### Interfaces Principais

```ts
export function matchesQuery(query: string, tags: Id3Tags): boolean
export function filterPlaylist(files: string[], query: string, id3Cache: Map<string, Id3Tags>): string[]
```

### Modelos de Dados

- `TAG_MAP: '<artist>'→artist, '<title>'|'<song>'→title, '<album>', '<year>', '<genre>', '<track>', '<disc>'`.
- Estado do hook: `query, searchFocused, saveName, saveOpen, saving, saveMessage`.

### Endpoints de API

- `POST /playlist {name, paths}` → `200 "Saved"` (ver `ofy-011`).

## Pontos de Integração

- Nenhuma externa.

## Abordagem de Testes

### Testes Unitários

- `SearchBar.test.tsx` cobre a UI. Recomendado (não existente): testes de `matchesQuery` para `&&`, `||`, `!=` com lista, comparação numérica vs. string.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Busca avançada introduzida na v1.3.0 (branch `feature/busca-avancada`), depois reutilizada no editor de playlists (1.4).

### Dependências Técnicas

- `id3Cache` carregado (`ofy-003`/`ofy-006`).

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- Filtragem local para resposta instantânea e sem novos endpoints.
- Regex escapada para texto livre (evita injeção de regex pelo usuário).

### Riscos Conhecidos

- Para bibliotecas muito grandes o filtro roda a cada tecla (sem debounce).
- Termos de tag inválidos (`<foo> == x`) retornam `false` silenciosamente (lista vazia).

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/shared/lib/search.ts`, `frontend/src/features/playlist-view/lib/usePlaylistSearch.ts`, `frontend/src/features/playlist-view/ui/SearchBar.tsx`, `frontend/src/features/collection/ui/PlaylistManager.tsx`
- Teste: `frontend/src/features/playlist-view/__tests__/ui/SearchBar.test.tsx`
