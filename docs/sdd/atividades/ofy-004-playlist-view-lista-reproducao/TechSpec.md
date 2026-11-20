# Especificação Técnica — ofy-004 — Lista de reprodução (Playlist view)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Componente `Playlist.tsx` compõe hooks de resize, tooltip e busca (`ofy-005`), consome `LibraryContext` (`playlistFiles`, `id3Cache`, `playlists`, estado de carregamento) e `PlayerContext` (`currentFile`). Ações de playlist usam `shared/api/playlist.ts`; abrir pasta usa `@tauri-apps/plugin-opener`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `features/playlist-view/ui/{Playlist.tsx, ColumnHeader.tsx, PlaylistTooltip.tsx, SearchBar.tsx}`
- `features/playlist-view/lib/{useColumnResize.ts, usePlaylistTooltip.ts, usePlaylistSearch.ts}`
- `shared/ui/ContextMenu.tsx` (`ContextMenuItem`, `ContextMenuSubmenu`, `ContextMenuSubmenuItem`, `getContextMenuPosition`, `useContextMenuClose`)
- Fluxo "Adicionar à playlist": contexto → `playlistApi.loadVirtual(name)` → checa duplicata → `playlistApi.savePlaylist(name, [...paths, file])` → mensagem.

## Design de Implementação

### Interfaces Principais

```ts
useColumnResize(initialArtistPercentage = 30, initialTimePixels = 62)
  // => { artistPercentage, timePixels, dragType, headerRef, startResize(type), gridStyle }
usePlaylistTooltip() // => { tooltip, tooltipStyle, tooltipRef, handleEnter(file), handleLeave }
getContextMenuPosition(e, menuElement): { x: number; y: number }
```

### Modelos de Dados

- Estado local: `songContextMenu {x, y, file}`, `addMessage`, `tooltip {file,x,y}`; larguras `artistPercentage`, `timePixels`.
- Dados: `Id3Tags` (`shared/types`).

### Endpoints de API

Consome `GET /playlist/{name}`, `POST /playlist {name, paths}` e `GET /playlists` (ver `ofy-011`).

## Pontos de Integração

- Plugin Tauri `opener` (permissão `opener:default` em `capabilities/default.json`).

## Abordagem de Testes

### Testes Unitários

- `Playlist.test.tsx`, `ColumnHeader.test.tsx`, `PlaylistTooltip.test.tsx`, `SearchBar.test.tsx`, `useColumnResize.test.ts`, `usePlaylistTooltip.test.ts` (Vitest + Testing Library).

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Colunas/tooltips/refatoração em Feature-Sliced (1.5) → menu de contexto com ícones (1.4) → "adicionar à playlist" (1.6.0).

### Dependências Técnicas

- `ofy-001` (tocar), `ofy-003` (dados), `ofy-011` (playlists virtuais), plugin `opener`.

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- CSS grid com `gridTemplateColumns` dinâmico para colunas redimensionáveis.
- Tooltip com atraso de 1 s e `requestAnimationFrame` para medir antes de posicionar.

### Riscos Conhecidos

- Renderização completa da lista em bibliotecas grandes.
- Listeners de menu de contexto potencialmente duplicados (ver Questões em Aberto do PRD).
- `navigator.clipboard` pode falhar silenciosamente (erro ignorado).

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/features/playlist-view/**`, `frontend/src/shared/ui/ContextMenu.tsx`, `frontend/src/shared/lib/format.ts`
- `frontend/src/widgets/RightPanel.tsx`, `frontend/src/app/App.css`
- Testes: `frontend/src/features/playlist-view/__tests__/**`
