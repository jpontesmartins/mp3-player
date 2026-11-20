# Especificação Técnica — ofy-003 — Carregamento de pasta e biblioteca

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

`useLoadFolder` encapsula o carregamento e a restauração; `LibraryContext.loadFolder` orquestra varredura (`GET /playlist`) e leitura de tags em lotes (`POST /id3/bulk`). No backend, `PlaylistController → PlaylistService.scanFolder → MusicScanner (FileMusicScanner)` usa `Files.walk`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- **Frontend:** `features/load-folder/lib/useLoadFolder.ts`, `app/providers/LibraryContext.tsx`, `features/settings/ui/SettingsPanel.tsx` (formulário), `app/App.tsx` (restauração com tentativas), `shared/api/playlist.ts` (`loadFolder`), `shared/api/id3.ts` (`bulkId3`).
- **Backend:** `PlaylistController#getPlaylist`, `PlaylistService#scanFolder`, `FileMusicScanner#scanFolder`.
- **Fluxo:** Carregar → `GET /playlist?path=` → `Set` único → `setPlaylistFiles/LibraryFiles` + `localStorage` → para cada lote de 50: `POST /id3/bulk?refresh=…` → merge imutável em `id3Cache` → `id3Loaded`. Ao terminar, `id3Loading=false` e `id3Total` zera após 1 s.

## Design de Implementação

### Interfaces Principais

```java
public interface MusicScanner { List<MusicFile> scanFolder(String folderPath) throws IOException; }
```
```ts
loadFolder(folder: string, forceRefresh?: boolean): Promise<boolean>   // LibraryContext
loadFolderWithRetry(folder: string, forceRefresh?: boolean): Promise<boolean>
restoreLastFolder(): Promise<boolean>
```

### Modelos de Dados

- `LibraryState { libraryFiles, playlistFiles, playlists, id3Cache: Map<string, Id3Tags>, id3Loading, id3Loaded, id3Total }`.
- Constante `ID3_BATCH_SIZE = 50`; chave `mp3_folder`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /playlist?path=<pasta>` | query `path` | `200 ["C:\\...\\a.mp3", ...]`; `400 "Error: Directory not found: …"` |
| `POST /id3/bulk?refresh=<bool>` | JSON `string[]` | `{ "<caminho>": { tags } }` (ver `ofy-006`) |

## Pontos de Integração

- Sistema de arquivos local (leitura). Nenhuma integração externa.

## Abordagem de Testes

### Testes Unitários

- Backend: `FileMusicFileScannerTest`, `PlaylistServiceTest`, `PlaylistControllerTest`.
- Frontend: não há teste específico de `useLoadFolder`/`LibraryContext` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Varredura e carregamento desde 1.0 → leitura de tags com virtual threads e cache (1.2) → restauração da última pasta com tentativas (backend iniciando junto do app).

### Dependências Técnicas

- Backend disponível em `localhost:8111`; `ofy-006` (tags/cache).

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixo `[Playlist]` ("Carregando pasta", "N arquivos encontrados") e `[ID3]` (bulk).

## Considerações Técnicas

### Decisões Principais

- Lotes de 50 com `await` sequencial: simples e mantém a UI atualizando progressivamente.
- Restauração com nova tentativa por 15 s para cobrir a inicialização do JAR embarcado.

### Riscos Conhecidos

- Pastas muito grandes: `Files.walk` carrega a lista completa em memória e o frontend mantém `Map` de tags de todos os arquivos.
- Sem validação de caminho (qualquer diretório legível pode ser varrido) — ver `Rules.md` §6.
- `loadVirtualPlaylist` captura `libraryFiles` e `id3Cache` por closure (`useCallback` com dependências) — pode usar valores defasados durante um carregamento em curso.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `frontend/src/features/load-folder/lib/useLoadFolder.ts`, `frontend/src/app/providers/LibraryContext.tsx`, `frontend/src/app/App.tsx`
- `frontend/src/features/settings/ui/SettingsPanel.tsx`, `frontend/src/shared/api/{playlist.ts,id3.ts}`
- `backend/src/main/java/com/ovelha/fy/player/playlist/{web/PlaylistController.java, application/PlaylistService.java, infrastructure/FileMusicScanner.java, domain/port/MusicScanner.java}`
