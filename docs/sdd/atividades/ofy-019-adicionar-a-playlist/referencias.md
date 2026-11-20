# Referências — ofy-019 — "Nova" playlist no menu "Adicionar à playlist"

## Origem da demanda

`docs/sdd/atividades/ofy-019-adicionar-a-playlist/input.txt` (a demanda está em arquivo único, não em pasta `input/`):

> adicionar no menu de contexto de uma música na "Playlist" da tela principal, dentro do item de adicionar "adicionar à playlist" o subitem "nova". ao clicar em nova deve aparecer uma modal para a pessoa dar o nome da nova playlist, com dois botões: salvar e cancelar.

## Código analisado

- `frontend/src/features/playlist-view/ui/Playlist.tsx` — menu `#song-context-menu` (linhas ~151–164) com `ContextMenuSubmenu` "Adicionar à playlist"; `addToPlaylist(name, file)` (linhas ~68–81) faz `loadVirtual` → checa duplicata de música → `savePlaylist(name, [...paths, file])`; `showAddMessage` (timer de 4 s, `.playlist-save-msg`); `handleSongContextMenu` chama `library.refreshPlaylists()`.
- `frontend/src/shared/ui/ContextMenu.tsx` — `ContextMenuItem`, `ContextMenuSubmenu`, `ContextMenuSubmenuItem` (`disabled?`), `getContextMenuPosition`, `useContextMenuClose` (chamado fora de `useEffect`, cleanup descartado — ver risco na TechSpec).
- `frontend/src/shared/ui/Modal.tsx` — `Modal({ onClose, overlayClass, dialogClass, children })`: fecha com `Esc` e clique no overlay; `stopPropagation` na caixa.
- `frontend/src/features/collection/ui/PlaylistManager.tsx` — modal de confirmação inline (`confirm-overlay`/`confirm-dialog`/`confirm-actions`, botões `pmanager-btn`); validação de `save` (nome vazio, `trim`); `startNew`.
- `frontend/src/features/playlist-view/ui/SearchBar.tsx` + `lib/usePlaylistSearch.ts` — fluxo atual de "salvar resultado como playlist" (Enter confirma, Esc cancela, botão desabilitado com nome vazio ou `saving`, rótulo "...").
- `frontend/src/shared/api/playlist.ts` — `listPlaylists`, `loadVirtual`, `savePlaylist(name, paths)`, `deletePlaylist`; `client.ts` — `postJSON` devolve `boolean`, `fetchJSON` devolve `null` em erro.
- `frontend/src/app/providers/LibraryContext.tsx` — `playlists: string[]`, `refreshPlaylists()`.
- `frontend/src/app/App.css` — `.confirm-overlay` (z-index 1000), `.confirm-dialog`, `.confirm-text`, `.confirm-actions`, `.pmanager-btn(.primary)`, `.playlist-save-msg`.
- `frontend/src/features/playlist-view/__tests__/ui/Playlist.test.tsx` — padrão de mocks (`shared/api/playlist`, `@tauri-apps/plugin-opener`), `renderPlaylistWithFiles`, `describe('context menu')` a estender.
- Backend (somente leitura): `PlaylistController` (`GET /playlists`, `POST /playlist` → `200 "Saved"`/`400`; `name` em branco → `400 "Missing playlist name"`), `PlaylistService.createOrUpdate`, `FilePlaylistRepository` (`~/.mp3-player/playlists/<sanitize(nome)>.txt`; `save` sobrescreve; `list` remove `.txt`).
- Specs relacionadas: `docs/sdd/atividades/ofy-004-*` (menu de contexto), `ofy-005-*` (salvar busca como playlist), `ofy-011-playlists-virtuais`, `docs/sdd/Rules.md` §2–§4 e §6, `docs/sdd/TechSpec.md` (tabela de endpoints e persistência).

## Contratos

`POST /playlist`
```json
{ "name": "Viagem", "paths": ["C:\\music\\a.mp3"] }
```
→ `200 "Saved"` (sobrescreve se o arquivo já existir) · `400` em falha.

`GET /playlists` → `["Pop", "Rock"]` (nomes de arquivo sem `.txt`, ordenados).

Sanitização do backend (`FilePlaylistRepository.sanitize`): `name.trim().replaceAll("[\\\\/:*?\"<>|]", "_")`; resultado vazio → `"playlist"`.

## Decisões técnicas tomadas no planejamento

1. Só frontend; nenhum endpoint novo (contratos protegidos em Rules §6).
2. Duplicidade checada na UI com `GET /playlists` imediatamente antes do `POST`, comparando nomes sanitizados sem diferenciar caixa (NTFS), pois o `POST` sobrescreve.
3. Nova playlist já nasce com a música clicada (premissa P1 do PRD).
4. Reuso do `Modal` compartilhado + classes `confirm-*`; estados e regras em `useNewPlaylist` / `playlistName.ts` (testáveis).
5. Rótulo "Nova…" e item de aviso "Nenhuma playlist criada" mantido (premissas P2 e P4 do PRD).
6. `useContextMenuClose` com cleanup descartado registrado como `[PENDENTE / AMBÍGUO NO CÓDIGO]`, fora de escopo.

## Pendências de validação antes do `/dev`

- P1 (conteúdo inicial = música clicada), P2 (rótulo "Nova…" vs "Nova"), P3 (bloquear duplicado vs adicionar à existente), P4 (manter aviso "Nenhuma playlist criada").
