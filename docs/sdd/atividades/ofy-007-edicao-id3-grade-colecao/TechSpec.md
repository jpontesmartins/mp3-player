# Especificação Técnica — ofy-007 — Edição de tags ID3 em grade (Coleção)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

`CollectionManager` mantém `edits: Map<arquivo, linha>` inicializado a partir de `id3Cache` quando a seleção muda. `useBulkEdit.updateTags` chama `id3Api.updateId3` sequencialmente. No backend, `MetadataController#updateId3 → Id3Service.update → CachedId3Codec.update → Id3MagicCodec.update`, que grava o arquivo e atualiza o cache.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Frontend: `features/collection/ui/CollectionManager.tsx` (grade), `features/collection/lib/useBulkEdit.ts` (`EDITABLE_FIELDS`, `fromTags`, `isDirty`, `updateTags`), `features/collection/lib/useAlbums.ts`, `shared/api/id3.ts#updateId3`.
- Backend: `MetadataController`, `Id3Service`, `CachedId3Codec`, `Id3MagicCodec`.
- `Id3MagicCodec.update`: `new ID3v24Tag()` → `setTitle/Artist/Album/Year` → gênero (`ID3v1Genres.matchGenreDescription` ou descrição livre) → `setTrack` → `setPartOfSet` (disco) → `setId3v2Tag` → `removeId3v1Tag` → `save(<arquivo>.mp3tmp)` → `Files.move(REPLACE_EXISTING)` → `read(path)`.

## Design de Implementação

### Interfaces Principais

```ts
updateId3(path: string, tags: Record<string,string>): Promise<Id3Tags | null>
updateTags(changed: Array<{file: string; tags: Record<string,string>}>): Promise<{successCount: number; failureCount: number}>
```
```java
MusicFile update(String filePath, Map<String,String> tags);   // Id3Codec
```

### Modelos de Dados

- `EditableField = 'title'|'artist'|'album'|'genre'|'track'|'disc'|'year'`.
- Requisição: `Id3UpdateRequest(String path, Map<String,String> tags)`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `POST /id3/update` | `{ "path": "...", "tags": { "title": "...", "artist": "...", "album": "...", "genre": "...", "track": "...", "disc": "...", "year": "..." } }` | `200` mapa de tags relido; `400 "Error: ..."` |

## Pontos de Integração

- mp3agic (escrita em arquivo).

## Abordagem de Testes

### Testes Unitários

- `Id3MagicCodecTest`, `CachedId3CodecTest`, `Id3ServiceTest`, `MetadataControllerTest`.
- Frontend: sem testes para `CollectionManager`/`useBulkEdit` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Edição individual (1.0) → gerenciador de coleção em grade (1.0/1.3) → reaproveitado por edição em massa (`ofy-008`).

### Dependências Técnicas

- `ofy-006` (leitura/cache) e `ofy-010` (seleção de álbum/artista).

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- `[Metadata] ID3 atualizado: <arquivo>` e `[ID3] Cache atualizado para: <arquivo>`.

## Considerações Técnicas

### Decisões Principais

- Gravar via arquivo temporário + `move` para não corromper o original em caso de falha.
- Atualizar o cache imediatamente após gravar (`CachedId3Codec.update`).

### Riscos Conhecidos

- **Perda de dados:** recriação da tag descarta frames não tratados e campos não enviados.
- **Arquivo em uso:** salvar a música em reprodução pode falhar no Windows (substituição de arquivo aberto) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- **Atomicidade parcial:** com várias linhas, falhas parciais deixam parte dos arquivos alterados.
- Exceções do codec são convertidas em `IllegalStateException("Error updating ID3 tags")` → `400`.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `frontend/src/features/collection/ui/CollectionManager.tsx`, `frontend/src/features/collection/lib/{useBulkEdit.ts,useAlbums.ts}`, `frontend/src/shared/api/id3.ts`
- `backend/src/main/java/com/ovelha/fy/player/music/{web/MetadataController.java, application/Id3Service.java, infrastructure/{Id3MagicCodec,CachedId3Codec}.java}`
