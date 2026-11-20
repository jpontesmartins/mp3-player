# Code Review — ofy-019 — "Nova" playlist no menu "Adicionar à playlist"

- **Atividade:** ofy-019 (inferida da conversa e da branch `feature/ofy-019-adicionar-a-playlist`; o skill foi invocado sem argumento).
- **Data:** 2026-11-20
- **Veredito Geral:** **APROVADO** (0 bloqueantes, 0 importantes, 2 sugestões)
- **Validação:** `npm run test:run` → 14 arquivos, 109 testes passando; `tsc --noEmit` sem erros.

## Escopo Analisado

Criados:
- `frontend/src/features/playlist-view/lib/playlistName.ts`
- `frontend/src/features/playlist-view/lib/useNewPlaylist.ts`
- `frontend/src/features/playlist-view/ui/NewPlaylistModal.tsx`
- `frontend/src/features/playlist-view/__tests__/lib/playlistName.test.ts`
- `frontend/src/features/playlist-view/__tests__/lib/useNewPlaylist.test.ts`
- `frontend/src/features/playlist-view/__tests__/ui/NewPlaylistModal.test.tsx`

Alterados:
- `frontend/src/features/playlist-view/ui/Playlist.tsx`
- `frontend/src/features/playlist-view/__tests__/ui/Playlist.test.tsx`
- `frontend/src/app/App.css`

Fora do escopo (não avaliados): alterações pendentes em `.claude/skills/` (`code-review` renomeado para `SKILL.md`, `fix-review` modificado) — não pertencem à ofy-019 e não devem entrar no mesmo commit.

## Checklist de Critérios de Aceite (`PRD.md`)

- [x] RF-019.1 — "Nova…" é o primeiro item do submenu, sempre habilitado (`Playlist.tsx`; teste "shows Nova… as first submenu item").
- [x] RF-019.2 — separador + playlists existentes ou "Nenhuma playlist criada" preservado (teste cobre lista vazia; teste existente cobre lista preenchida).
- [x] RF-019.3 — clicar em "Nova…" fecha o menu e abre a modal com a música clicada (teste "creates a new playlist through the modal").
- [x] RF-019.4 — título, campo com placeholder e foco automático, Cancelar/Salvar (teste da modal; `role="dialog"`, `aria-modal`, `aria-labelledby`, `aria-label`).
- [x] RF-019.5 — Salvar desabilitado com nome vazio ou salvando; rótulo "..." (código; desabilitado vazio coberto por teste, "..." sem teste dedicado).
- [x] RF-019.6 — Enter salva; Esc, Cancelar e overlay fecham sem gravar (teste da modal).
- [x] RF-019.7 — nome com `trim` enviado ao backend (teste do hook).
- [x] RF-019.8 — duplicado bloqueado, comparação sanitizada e case-insensitive, fallback para lista em memória (testes de `playlistName` e do hook).
- [x] RF-019.9 — sucesso: grava `[caminho]`, fecha, atualiza lista, mensagem `Playlist "X" criada com "a.mp3"` (testes do hook e de `Playlist`).
- [x] RF-019.10 — falha ao gravar mantém a modal aberta com "Erro ao salvar playlist" (teste do hook).
- [x] RF-019.11 — erro some ao editar o campo (teste do hook).

## Achados

### SUGESTÃO

- **`[CR-01]` `[SUGESTÃO]` `[NEGÓCIO]` — Cancelar durante a gravação não impede a criação**
  - **Local:** `frontend/src/features/playlist-view/lib/useNewPlaylist.ts:30-45`, `frontend/src/features/playlist-view/ui/NewPlaylistModal.tsx:41`
  - **Referência:** PRD RF-019.6 ("fecham sem gravar nada").
  - **Problema:** com a gravação em andamento, Esc/Cancelar/clique fora fecham a modal, mas o `POST /playlist` já enviado conclui, a playlist é criada e a mensagem de sucesso aparece. A janela é de milissegundos (backend local), por isso não é bloqueante.
  - **Correção sugerida:** ignorar `onClose` enquanto `saving` é verdadeiro (ou desabilitar Cancelar nesse intervalo), ou aceitar o comportamento e documentá-lo na spec.

- **`[CR-02]` `[SUGESTÃO]` `[ARQUITETURA]` — Cor de erro nova em vez de reutilizar a existente**
  - **Local:** `frontend/src/app/App.css:2076` (`.new-playlist-error`, `#e57373`)
  - **Referência:** `Rules.md` §3 (CSS global com tokens/estilo existente); TechSpec "Arquivos relevantes".
  - **Problema:** o projeto já tem `.pmanager-error` com `#e07b7b`; a nova classe introduz um tom vermelho levemente diferente para o mesmo fim.
  - **Correção sugerida:** usar `#e07b7b` (ou reaproveitar a classe `pmanager-error`) para manter consistência visual.

## Sincronia de Documentação

- `PRD.md` e `TechSpec.md` não precisam de ajuste obrigatório; a implementação seguiu o desenho (arquivos, assinaturas e fluxo de `submit()` idênticos).
- Opcional: se CR-01 for aceito como está, registrar em `TechSpec.md` (Riscos Conhecidos) que cancelar durante a gravação não aborta a requisição.
- As premissas P1–P4 do PRD foram implementadas como escritas; seguem pendentes de confirmação do usuário.
