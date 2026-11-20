# TechSpec — ofy-019 — "Nova" playlist no menu "Adicionar à playlist"

## Resumo Executivo

Mudança exclusivamente no frontend, na feature `playlist-view`. O submenu "Adicionar à playlist" de `Playlist.tsx` ganha o item "Nova…", que fecha o menu e abre um novo componente `NewPlaylistModal` (sobre o `Modal` compartilhado de `shared/ui/Modal.tsx`). A orquestração (validação de nome duplicado, chamada à API, mensagens) fica num hook `useNewPlaylist` em `features/playlist-view/lib/`, e a regra pura de nome (trim + sanitização espelhando o backend + comparação case-insensitive) fica em `features/playlist-view/lib/playlistName.ts`, ambos testados com Vitest.

Nenhum endpoint novo: a criação usa `POST /playlist` (`playlistApi.savePlaylist(name, [file])`) e a checagem de duplicidade usa `GET /playlists` (`playlistApi.listPlaylists()`), já que o backend sobrescreve playlists de mesmo nome sem aviso. Após sucesso, `library.refreshPlaylists()` atualiza o estado global e a mensagem de 4 s reaproveita `showAddMessage` já existente em `Playlist.tsx`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- **`features/playlist-view/ui/Playlist.tsx` (alterado)** — adiciona o `ContextMenuSubmenuItem` "Nova…" + separador no submenu; mantém estado `newPlaylistFile: string | null`; renderiza `NewPlaylistModal` quando não nulo; passa `onCreated` que chama `showAddMessage`.
- **`features/playlist-view/ui/NewPlaylistModal.tsx` (novo)** — apresentação pura: título, input com foco automático, mensagem de erro, botões Cancelar/Salvar. Recebe estado/handlers de `useNewPlaylist`.
- **`features/playlist-view/lib/useNewPlaylist.ts` (novo)** — estado do formulário (`name`, `error`, `saving`) e `submit()`: valida, consulta playlists, grava, atualiza a biblioteca e notifica sucesso.
- **`features/playlist-view/lib/playlistName.ts` (novo)** — funções puras `sanitizePlaylistName` e `isDuplicatePlaylistName`.
- **`shared/ui/ContextMenu.tsx`, `shared/ui/Modal.tsx`, `shared/api/playlist.ts`, `app/providers/LibraryContext.tsx`** — reutilizados sem alteração.
- **`app/App.css` (alterado)** — estilos mínimos para título, input e erro da modal (`.new-playlist-*`), reaproveitando tokens `--c-*` e `confirm-overlay`/`confirm-dialog`/`confirm-actions`.

Fluxo de dados:

```
contextmenu na <li> → Playlist (songContextMenu.file)
  └─ clique "Nova…" → setSongContextMenu(null); setNewPlaylistFile(file)
       └─ <NewPlaylistModal> ← useNewPlaylist({ file, existing: library.playlists, refresh, onCreated, onClose })
            └─ submit(): listPlaylists() → isDuplicatePlaylistName? → savePlaylist(name,[file])
                 → refreshPlaylists() → onCreated(name) → showAddMessage(...) → onClose()
```

## Design de Implementação

### Interfaces Principais

```ts
// features/playlist-view/lib/playlistName.ts
/** Espelha FilePlaylistRepository.sanitize: trim + [\\/:*?"<>|] → '_'; vazio → 'playlist'. */
export function sanitizePlaylistName(name: string): string;
/** true se sanitize(name) coincide (case-insensitive) com algum nome existente sanitizado. */
export function isDuplicatePlaylistName(name: string, existing: string[]): boolean;
```

```ts
// features/playlist-view/lib/useNewPlaylist.ts
interface UseNewPlaylistOptions {
  file: string;                       // caminho absoluto da música clicada
  existing: string[];                 // library.playlists (fallback)
  refresh: () => Promise<void>;       // library.refreshPlaylists
  onCreated: (name: string) => void;  // exibe mensagem em Playlist
  onClose: () => void;                // fecha a modal
}
export function useNewPlaylist(o: UseNewPlaylistOptions): {
  name: string; setName(v: string): void;  // setName limpa error (RF-019.11)
  error: string; saving: boolean; canSave: boolean;
  submit(): Promise<void>;
};
```

```tsx
// features/playlist-view/ui/NewPlaylistModal.tsx (export default)
interface Props { file: string; onClose(): void; onCreated(name: string): void; }
```

Algoritmo de `submit()`:
1. `trimmed = name.trim()`; se vazio ou `saving` → retorna.
2. `setSaving(true)`; `names = (await playlistApi.listPlaylists()) ?? existing`.
3. Se `isDuplicatePlaylistName(trimmed, names)` → `error = 'Já existe uma playlist com esse nome'`, `saving=false`, retorna.
4. `ok = await playlistApi.savePlaylist(trimmed, [file])`; se `!ok` → `error = 'Erro ao salvar playlist'`, `saving=false`, retorna.
5. `await refresh()`; `onCreated(trimmed)`; `onClose()`.

Mensagem de sucesso em `Playlist.tsx`: `` showAddMessage(`Playlist "${name}" criada com "${fileName(file)}"`) ``.

### Modelos de Dados

- Sem novos tipos de API. Payload enviado (contrato existente): `POST /playlist` `{ "name": "<trimmed>", "paths": ["<caminho absoluto>"] }` → `200 "Saved"` / `400`.
- Estado local novo em `Playlist.tsx`: `newPlaylistFile: string | null`.
- Persistência resultante: `~/.mp3-player/playlists/<nome sanitizado>.txt` com uma linha (o caminho). Formato inalterado.

### Endpoints de API

Nenhum endpoint novo ou alterado. Consumidos:

| Método/Path | Uso nesta atividade | Função |
|---|---|---|
| `GET /playlists` | checagem de duplicidade no momento de salvar | `playlistApi.listPlaylists()` |
| `POST /playlist` | criação da playlist com 1 música | `playlistApi.savePlaylist(name, [file])` |

## Pontos de Integração

Nenhuma integração externa. Erros de rede são tratados conforme o padrão do `shared/api/client.ts` (retorno `null`/`false`): `listPlaylists` nulo → fallback para `library.playlists`; `savePlaylist` falso → mensagem de erro na modal (RF-019.10).

## Abordagem de Testes

### Testes Unitários

- **`__tests__/lib/playlistName.test.ts`** (novo):
  - `sanitizePlaylistName`: trim; troca de cada caractere `\ / : * ? " < > |` por `_`; string vazia/só espaços → `'playlist'`.
  - `isDuplicatePlaylistName`: igual exato; diferença só de caixa (`rock` vs `Rock`) → true; `a/b` vs existente `a_b` → true; nome inédito → false; lista vazia → false.
- **`__tests__/lib/useNewPlaylist.test.ts`** (novo, `renderHook`, mock de `shared/api/playlist`):
  - `canSave` falso com nome vazio/só espaços.
  - duplicado → `error` definido, `savePlaylist` não chamado, `onClose` não chamado.
  - `listPlaylists` retorna `null` → usa `existing` como fallback.
  - sucesso → `savePlaylist('Viagem', [file])` com nome trimado, `refresh`, `onCreated('Viagem')`, `onClose` chamados.
  - `savePlaylist` falso → `error = 'Erro ao salvar playlist'`, `saving` volta a `false`, modal não fecha.
  - `setName` limpa `error`.
- **`__tests__/ui/NewPlaylistModal.test.tsx`** (novo):
  - renderiza título, input focado, botões; Salvar desabilitado com campo vazio.
  - Enter no campo salva; Esc, Cancelar e clique no overlay chamam `onClose` sem chamar `savePlaylist`.
  - erro exibido com `role="alert"`.
- **`__tests__/ui/Playlist.test.tsx`** (alterado; adicionar casos no `describe('context menu')`):
  - "Nova…" aparece como primeiro item do submenu (com e sem playlists existentes); "Nenhuma playlist criada" continua aparecendo quando a lista é vazia.
  - clicar em "Nova…" fecha `#song-context-menu` e abre a modal (`role="dialog"`).
  - fluxo completo: digitar nome + Salvar → `savePlaylist(nome, ['C:\\music\\a.mp3'])` e mensagem `Playlist "X" criada com "a.mp3"`.
  - casos existentes (listar, adicionar, não duplicar, hint) continuam passando.
- Mocks: apenas `shared/api/playlist` e `@tauri-apps/plugin-opener` (já mockados no arquivo).

### Testes de Integração

Não há suíte E2E no projeto. Verificação manual na app (`npm run dev:vite` + backend `mvn spring-boot:run`): criar playlist via "Nova…", conferir arquivo em `~/.mp3-player/playlists/`, nome aparecendo no submenu e em Coleção → Playlists; tentar nome duplicado; cancelar por Esc/clique fora; backend parado → mensagem de erro.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

1. `lib/playlistName.ts` + testes (regra pura, base da validação).
2. `lib/useNewPlaylist.ts` + testes (depende de 1 e de `shared/api/playlist`).
3. `ui/NewPlaylistModal.tsx` + estilos em `App.css` + testes (depende de 2).
4. Integração em `Playlist.tsx` (item "Nova…", separador, estado `newPlaylistFile`, mensagem) + atualização de `Playlist.test.tsx`.
5. `npm run test:run` e `tsc` (strict) no frontend; verificação manual.

### Dependências Técnicas

- Nenhuma dependência nova de pacote. Backend em execução apenas para verificação manual.

## Monitoramento e Observabilidade

O projeto não usa Prometheus/Grafana. O backend já registra `[Playlist] Playlist salva: <nome> (1 músicas)` em `FilePlaylistRepository.save`; nenhuma métrica ou log novo no frontend (padrão do projeto: sem logging no cliente).

## Considerações Técnicas

### Decisões Principais

- **Validação de duplicidade na UI, não no backend:** `POST /playlist` é contrato protegido e é usado também para *atualizar* (adicionar música a existente, editor da Coleção). Mudar a semântica exigiria novo endpoint; a checagem no cliente com `GET /playlists` imediatamente antes de salvar atende ao requisito sem alterar contratos.
- **Espelhar a sanitização do backend no frontend:** `GET /playlists` devolve nomes já sanitizados (nomes de arquivo); comparar o nome digitado sanitizado evita que `a/b` sobrescreva `a_b`. Comparação case-insensitive porque o sistema de arquivos alvo (Windows/NTFS) não diferencia caixa — `Rock.txt` e `rock.txt` são o mesmo arquivo.
- **Componente `Modal` compartilhado** em vez de repetir o markup inline do `PlaylistManager` (que não trata Esc): ganha Esc e clique-fora prontos. Classes `confirm-overlay`/`confirm-dialog` reutilizadas para manter a identidade visual.
- **Hook separado (`useNewPlaylist`)** em vez de lógica dentro de `Playlist.tsx`: segue o padrão `ui/` + `lib/` da feature e permite testar sem renderizar a lista.
- **Rejeitadas:** (a) reaproveitar a linha inline de "salvar resultado" da `SearchBar` — a demanda pede modal; (b) criar a playlist vazia e depois adicionar — duas chamadas sem ganho.

### Riscos Conhecidos

- **Condição de corrida** entre a checagem `GET /playlists` e o `POST`: outra janela/ação poderia criar o mesmo nome no intervalo. App de usuário único → risco aceito.
- **Divergência da sanitização** se `FilePlaylistRepository.sanitize` mudar: mitigar com comentário apontando para a classe Java e registro em `referencias.md`.
- **`useContextMenuClose` (shared/ui/ContextMenu.tsx)** é chamado fora de `useEffect` e o cleanup retornado é descartado: listeners de `mousedown`/`scroll`/`resize` acumulam enquanto o menu está aberto e não são removidos `[PENDENTE / AMBÍGUO NO CÓDIGO]`. Efeito prático aqui: um `mousedown` dentro da modal dispara `setSongContextMenu(null)` (inócuo, o menu já está fechado). Não corrigir nesta atividade; registrar.
- **Clique em "Nova…"**: o submenu para `mousedown` com `stopPropagation`, então o clique chega ao `onClick` do item antes de o menu fechar — mesmo mecanismo dos itens atuais (testado).
- **Overlay fecha no `click`**: arrastar a seleção de texto do input para fora da caixa pode disparar `click` no overlay e cancelar. Comportamento igual ao das demais modais; aceito.

### Conformidade com Padrões

O projeto não possui `.cursor/rules`; as regras aplicáveis estão em `docs/sdd/Rules.md`:
- §2 — `app → widgets → features → shared`; componente `ui/` sem `fetch` (usa `shared/api/playlist` via hook em `lib/`).
- §3 — componentes `PascalCase.tsx` com export default, hooks `useXxx.ts`, utilitários `camelCase.ts`, classes CSS `kebab-case`, textos pt-BR, mensagens temporárias de 4 s, CSS puro em `App.css` com tokens `--c-*`, MUI apenas para ícones.
- §4 — lógica nova em `lib/` com testes Vitest; `npm run test:run`.
- §6 — contratos HTTP e formato `.txt` das playlists inalterados; sanitização de nomes mantida no ponto de escrita (backend).
- `.opencode/skills/frontend-style/SKILL.md` — estilo monocromático.

### Arquivos relevantes

Criar:
- `frontend/src/features/playlist-view/lib/playlistName.ts`
- `frontend/src/features/playlist-view/lib/useNewPlaylist.ts`
- `frontend/src/features/playlist-view/ui/NewPlaylistModal.tsx`
- `frontend/src/features/playlist-view/__tests__/lib/playlistName.test.ts`
- `frontend/src/features/playlist-view/__tests__/lib/useNewPlaylist.test.ts`
- `frontend/src/features/playlist-view/__tests__/ui/NewPlaylistModal.test.tsx`

Alterar:
- `frontend/src/features/playlist-view/ui/Playlist.tsx`
- `frontend/src/features/playlist-view/__tests__/ui/Playlist.test.tsx`
- `frontend/src/app/App.css`

Somente leitura (reutilizados):
- `frontend/src/shared/ui/ContextMenu.tsx`, `frontend/src/shared/ui/Modal.tsx`
- `frontend/src/shared/api/playlist.ts`, `frontend/src/shared/api/client.ts`
- `frontend/src/app/providers/LibraryContext.tsx`
- `backend/src/main/java/com/ovelha/fy/player/playlist/infrastructure/FilePlaylistRepository.java` (regra de sanitização espelhada)
- `backend/src/main/java/com/ovelha/fy/player/playlist/web/PlaylistController.java`
