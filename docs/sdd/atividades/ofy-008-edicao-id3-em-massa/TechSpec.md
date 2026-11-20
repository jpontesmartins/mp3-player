# Especificação Técnica — ofy-008 — Edição de ID3 em massa por padrão de nome de arquivo

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Tudo é calculado no frontend (`BulkId3Editor.tsx`): compilação do padrão em regex, *parse* do nome, mescla com campos fixos e envio sequencial de `POST /id3/update` via `useBulkEdit.updateTags`. O backend é o mesmo de `ofy-007`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `features/collection/ui/BulkId3Editor.tsx` (`compilePattern`, `parseFilename`, `Row`), `features/collection/lib/useBulkEdit.ts`, `features/collection/ui/CollectionManager.tsx` (entrada + menu de contexto `album-context-menu`), `shared/lib/format.ts` (`escapeRegex`, `fileName`, `parentDirectory`), `shared/api/playlist.ts#loadFolder`.
- Fluxo: pasta → `GET /playlist` → arquivos `.mp3` → `rows = files.map(parse + merge)` → Aplicar → `updateId3` por arquivo → `library.updateId3Cache`.

## Design de Implementação

### Interfaces Principais

```ts
compilePattern(pattern: string): { keys: TagKey[]; regex: RegExp; isSuccess: boolean }
parseFilename(name: string, compiled): Partial<Record<TagKey,string>> | null
// aliases: title|song→title, artist, album, genre, track, disc|cd|disk→disc, year
```

### Modelos de Dados

- `Row { file, name, parsed | null, merged: Record<TagKey,string> }`; `TAGS = [title, artist, album, genre, track, disc, year]`.

### Endpoints de API

- `GET /playlist?path=` (`ofy-003`) e `POST /id3/update` (`ofy-007`).

## Pontos de Integração

- Nenhuma externa.

## Abordagem de Testes

### Testes Unitários

- Sem testes específicos `[PENDENTE / AMBÍGUO NO CÓDIGO]`. Recomendado: testes de `compilePattern`/`parseFilename` (padrões com literais, último placeholder guloso, placeholder desconhecido).

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Edição de ID3 em massa (1.1.0) → acesso direto pelo álbum da coleção (1.4.0).

### Dependências Técnicas

- `ofy-007` (gravação), `ofy-003` (varredura).

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- Pré-visualização local antes de gravar, para reduzir erros em operação destrutiva.
- Envio sequencial (simples, contagem de sucesso/falha por arquivo).

### Riscos Conhecidos

- Ver "Questões em Aberto" (perda de campos).
- Muitos arquivos → muitas gravações sequenciais; sem barra de progresso.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/features/collection/ui/{BulkId3Editor.tsx, CollectionManager.tsx}`, `frontend/src/features/collection/lib/useBulkEdit.ts`
- `frontend/src/shared/lib/format.ts`, `frontend/src/shared/api/{playlist.ts,id3.ts}`
