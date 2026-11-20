# Especificação Técnica — ofy-011 — Playlists virtuais (criação, edição e exclusão)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Backend: `PlaylistController → PlaylistService → PlaylistRepository (FilePlaylistRepository)`. Frontend: `PlaylistManager` (lista + editor com DnD por mouse), `LibraryContext.loadVirtualPlaylist`, `shared/api/playlist.ts`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Backend: `PlaylistController` (CRUD), `PlaylistService` (`list/load/createOrUpdate/delete/rename`), `FilePlaylistRepository` (`baseDir = ~/.mp3-player/playlists`, cria o diretório no construtor), `Playlist` (modelo imutável; `addSong/removeSong`; igualdade por nome).
- Frontend: `features/collection/ui/PlaylistManager.tsx`, `app/providers/LibraryContext.tsx` (`playlists`, `refreshPlaylists`, `loadVirtualPlaylist`), `shared/api/playlist.ts`, `features/playlist-view/**` (salvar por busca; adicionar por menu de contexto).
- Fluxo DnD: `mousedown` no item esquerdo → `dragFile` + ghost → `mouseup` sobre `rightPaneRef` → adiciona se ausente.

## Design de Implementação

### Interfaces Principais

```java
public interface PlaylistRepository { List<String> list(); List<String> load(String name); void save(Playlist p); void delete(String name); void rename(String from, String to); }
```
```ts
listPlaylists(): Promise<string[] | null>; loadVirtual(name): Promise<string[] | null>
savePlaylist(name: string, paths: string[]): Promise<boolean>; deletePlaylist(name: string): Promise<boolean>
```

### Modelos de Dados

- Arquivo: texto UTF-8, uma linha por caminho; linhas em branco são ignoradas na leitura (`trim` + filtro).
- `PlaylistSaveRequest(String name, List<String> paths)`, `PlaylistRenameRequest(String oldName, String newName)`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /playlists` | — | `["nome1", "nome2"]` |
| `GET /playlist/{name}` | — | `["caminho", ...]` (vazio se não existe) |
| `POST /playlist` | `{ "name": "...", "paths": ["..."] }` | `200 "Saved"`; `400 "Missing playlist name"` / `"Error: ..."` |
| `DELETE /playlist/{name}` | — | `200 "Deleted"`; `404` em exceção |
| `POST /playlist/rename` | `{ "oldName": "...", "newName": "..." }` | `200 "Renamed"`; `404` se não existe |

## Pontos de Integração

- Sistema de arquivos do usuário.

## Abordagem de Testes

### Testes Unitários

- `FilePlaylistRepositoryTest`, `PlaylistServiceTest`, `PlaylistControllerTest`, `PlaylistTest`.
- Frontend: sem testes do `PlaylistManager` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Testes de Integração

- Não há.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Playlists virtuais (1.0) → edição com duas colunas e arrastar/soltar (1.4.0) → salvar por busca (1.3.0) → adicionar pelo menu de contexto (1.6.0).

### Dependências Técnicas

- `ofy-003` (biblioteca), `ofy-005`, `ofy-004`.

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixo `[Playlist]` (salva, removida, renomeada, erros de IO).

## Considerações Técnicas

### Decisões Principais

- Persistência em `.txt` (legível e sem BD).
- DnD via eventos de mouse em vez de HTML5 Drag&Drop (controle total do "fantasma" e da área de soltura).

### Riscos Conhecidos

- Arquivos movidos/removidos deixam caminhos inválidos na playlist (sem verificação).
- Sobrescrita silenciosa ao salvar com nome existente.
- `loadVirtualPlaylist` depende de `libraryFiles`/`id3Cache` capturados em closure.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/player/playlist/{web/PlaylistController.java, application/PlaylistService.java, infrastructure/FilePlaylistRepository.java, domain/{model/Playlist,repository/PlaylistRepository}.java}`
- `frontend/src/features/collection/ui/PlaylistManager.tsx`, `frontend/src/app/providers/LibraryContext.tsx`, `frontend/src/shared/api/playlist.ts`
