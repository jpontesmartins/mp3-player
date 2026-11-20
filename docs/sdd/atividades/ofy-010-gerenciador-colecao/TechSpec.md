# Especificação Técnica — ofy-010 — Gerenciador de coleção (álbuns e artistas)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Somente frontend. `useAlbums` deriva `albums` e `artists` com `useMemo` a partir de `LibraryContext`; `CollectionManager` controla subtelas por flags locais (`playlistView`, `bulkView`) e o menu de contexto do álbum.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `features/collection/ui/CollectionManager.tsx`, `features/collection/lib/useAlbums.ts`, `widgets/LeftPanel.tsx` (seleciona o painel por `AppContext.view`), `widgets/Toolbar.tsx`, `shared/ui/ContextMenu.tsx`, plugin `opener` (`revealItemInDir`).
- Fluxo: `libraryFiles` → agrupar por `parentDirectory(file)` → nome por moda da tag `album` → ordenação; `artists` por contagem da tag `artist`.

## Design de Implementação

### Interfaces Principais

```ts
useAlbums(): { albums: Array<{folder: string; name: string; files: string[]}>; artists: Array<{name: string; count: number}> }
```

### Modelos de Dados

- `Selection { type: 'album'|'artist'; key: string; name: string; files: string[] | null }`.

### Endpoints de API

- Nenhum exclusivo (usa `GET /playlists`, ver `ofy-011`).

## Pontos de Integração

- Plugin Tauri `opener` para abrir a pasta.

## Abordagem de Testes

### Testes Unitários

- Sem testes `[PENDENTE / AMBÍGUO NO CÓDIGO]`. Recomendado: testes de `useAlbums` (moda do álbum, fallback para nome da pasta, ordenação).

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Coleção com álbuns/artistas (1.0) → opção "todos os arquivos" (1.3.0) → menu de contexto com ícones e edição em massa (1.4.0).

### Dependências Técnicas

- `ofy-003`, `ofy-006`.

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- Agrupar por pasta é barato e determinístico; a tag de álbum só define o rótulo.

### Riscos Conhecidos

- Recalculado a cada mudança de `id3Cache` (inclui atualizações do polling); em bibliotecas grandes pode custar renderizações.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/features/collection/ui/CollectionManager.tsx`, `frontend/src/features/collection/lib/useAlbums.ts`
- `frontend/src/widgets/{LeftPanel.tsx,Toolbar.tsx}`, `frontend/src/app/providers/{AppContext.tsx,LibraryContext.tsx}`
