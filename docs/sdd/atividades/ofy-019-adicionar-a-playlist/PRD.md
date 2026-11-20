# PRD — ofy-019 — "Nova" playlist no menu "Adicionar à playlist"

## Visão Geral

Hoje, ao clicar com o botão direito numa música da lista de reprodução ("Playlist") da tela principal, o submenu **Adicionar à playlist** só oferece as playlists virtuais que já existem (ou o aviso desabilitado "Nenhuma playlist criada"). Para colocar uma música numa playlist nova, a pessoa precisa sair da tela principal, ir em **Coleção → Playlists**, criar a playlist no editor de duas colunas e procurar a música de novo.

Esta atividade adiciona o subitem **"Nova…"** a esse submenu. Ao clicar nele, abre-se uma modal pedindo o nome da nova playlist, com os botões **Salvar** e **Cancelar**. Ao salvar, a playlist é criada já contendo a música clicada. O público é o dono da coleção (usuário único, interface em pt-BR); o valor é criar playlists sem sair do fluxo de escuta.

## Objetivos

- Criar uma playlist nova contendo a música clicada em no máximo 3 interações a partir do menu de contexto (botão direito → "Nova…" → digitar nome + Salvar/Enter).
- Nunca sobrescrever silenciosamente uma playlist existente ao criar uma "nova" (o backend `POST /playlist` sobrescreve por padrão).
- Não alterar contratos HTTP nem o formato de persistência das playlists (`~/.mp3-player/playlists/<nome>.txt`).
- Manter o comportamento atual de "adicionar a playlist existente" intacto (testes existentes continuam passando, salvo o ajuste descrito em RF-019.1).

## Histórias de Usuário

- Como **ouvinte**, quero clicar com o botão direito numa música e escolher "Adicionar à playlist → Nova…" para criar uma playlist com essa música sem sair da tela principal.
- Como **ouvinte**, quero poder cancelar a criação (botão Cancelar, `Esc` ou clique fora) sem que nada seja gravado.
- Como **organizador**, quero ser avisado se já existe uma playlist com o nome digitado, para não perder o conteúdo dela por engano.
- Caso extremo: como **ouvinte sem nenhuma playlist criada**, quero conseguir criar a primeira playlist pelo mesmo menu.
- Caso extremo: se o backend estiver fora do ar, quero ver uma mensagem de erro e continuar com a modal aberta para tentar de novo ou cancelar.

## Funcionalidades Principais

### F1 — Subitem "Nova…" no submenu "Adicionar à playlist"

- **RF-019.1** O submenu **Adicionar à playlist** do menu de contexto de música (`#song-context-menu`, tela principal) passa a exibir, como **primeiro** item, o subitem **"Nova…"**, sempre habilitado.
- **RF-019.2** Abaixo de "Nova…", um separador visual e, em seguida, a lista atual: as playlists existentes (em ordem retornada pelo backend) ou, se não houver nenhuma, o item desabilitado "Nenhuma playlist criada" (comportamento preservado).
- **RF-019.3** Clicar em "Nova…" fecha o menu de contexto e abre a modal de nova playlist, guardando a música (caminho absoluto) sobre a qual o menu foi aberto.

### F2 — Modal "Nova playlist"

- **RF-019.4** A modal exibe: título **"Nova playlist"**, um campo de texto com placeholder **"Nome da playlist"** (recebe foco automaticamente ao abrir) e dois botões: **Cancelar** e **Salvar** (nessa ordem, alinhados à direita, mesmo padrão visual da modal de confirmação de exclusão do gerenciador de playlists).
- **RF-019.5** **Salvar** fica desabilitado enquanto o nome, sem espaços nas extremidades, estiver vazio e enquanto a gravação estiver em andamento (rótulo "..." durante a gravação, como na barra de busca).
- **RF-019.6** `Enter` no campo equivale a clicar em Salvar (respeitando RF-019.5). `Esc`, o botão Cancelar e o clique no fundo escurecido (fora da caixa) fecham a modal sem gravar nada.

### F3 — Regras de criação

- **RF-019.7** O nome gravado é o texto digitado sem espaços nas extremidades (`trim`). A sanitização de caracteres inválidos continua sendo feita pelo backend (`[\\/:*?"<>|]` → `_`).
- **RF-019.8** **Nome duplicado:** se já existir uma playlist cujo nome, após aplicar a mesma sanitização do backend e comparar sem diferenciar maiúsculas/minúsculas, seja igual ao nome digitado, a playlist **não** é gravada; a modal permanece aberta exibindo a mensagem **"Já existe uma playlist com esse nome"**. A verificação usa a lista de playlists obtida do backend no momento de salvar (com fallback para a lista já carregada em memória se a consulta falhar).
- **RF-019.9** **Sucesso:** a playlist é criada contendo exatamente a música clicada (`[caminho]`); a modal fecha; a lista de playlists em memória é atualizada (o novo nome passa a aparecer no submenu e no gerenciador de Coleção); exibe-se por 4 s a mensagem **`Playlist "<nome>" criada com "<arquivo>.mp3"`** no mesmo local das mensagens atuais de "adicionar à playlist" (`.playlist-save-msg`).
- **RF-019.10** **Falha ao gravar** (backend indisponível ou resposta não-2xx): a modal permanece aberta com o nome digitado e exibe **"Erro ao salvar playlist"**; Salvar volta a ficar habilitado.
- **RF-019.11** Qualquer mensagem de erro dentro da modal some assim que o usuário altera o texto do campo.

## Experiência do Usuário

- **Persona:** dono da coleção ouvindo músicas na tela principal.
- **Fluxo principal:**
  1. Botão direito numa música da lista → menu de contexto.
  2. Passa o mouse em "Adicionar à playlist" → submenu com "Nova…", separador e playlists existentes.
  3. Clica em "Nova…" → menu fecha, modal "Nova playlist" abre com foco no campo.
  4. Digita "Viagem" → Enter (ou Salvar) → modal fecha, mensagem `Playlist "Viagem" criada com "musica.mp3"` por 4 s.
- **Fluxo alternativo (duplicado):** digita "rock" com playlist "Rock" existente → mensagem "Já existe uma playlist com esse nome" na modal; usuário altera o nome ou cancela.
- **Fluxo de cancelamento:** Cancelar / `Esc` / clique fora → modal fecha, nada é gravado, nenhuma mensagem é exibida.
- **UI:** estilo monocromático existente, reutilizando o overlay/caixa da modal de confirmação (`confirm-overlay`/`confirm-dialog`) e os botões `pmanager-btn`; ícones apenas MUI (nenhum ícone novo obrigatório). Textos em pt-BR.
- **Acessibilidade:** caixa da modal com `role="dialog"`, `aria-modal="true"` e `aria-labelledby` apontando para o título; campo com `aria-label="Nome da playlist"`; mensagem de erro com `role="alert"`; operação completa por teclado (foco automático, Enter, Esc).

## Restrições Técnicas de Alto Nível

- Somente frontend: reutiliza `GET /playlists` e `POST /playlist` sem alteração de contrato (`docs/sdd/TechSpec.md`, contratos protegidos em `Rules.md` §6).
- Componentes `ui/` não chamam `fetch`; a rede passa por `shared/api/playlist.ts` (Rules §2).
- Formato de persistência das playlists (`.txt`, um caminho por linha) inalterado; sanitização de nomes continua no backend.
- Lógica nova em `lib/` acompanha testes Vitest (Rules §4).

## Fora de Escopo

- Criar playlist nova a partir de outros menus (capa, álbum no gerenciador de Coleção) ou com várias músicas selecionadas.
- Alterar o editor de playlists da Coleção, renomear ou excluir playlists.
- Validação/unicidade de nomes no backend (o `POST /playlist` continua sobrescrevendo; a proteção é feita na UI).
- Limite de tamanho do nome ou bloqueio de caracteres na digitação (o backend já sanitiza).
- Abrir/carregar automaticamente a playlist recém-criada na lista de reprodução.
- Corrigir o hook `useContextMenuClose` (ver riscos na TechSpec) — só se torna escopo se impedir esta funcionalidade.

## Questões em Aberto

- **Premissa P1 — conteúdo inicial:** a demanda pede apenas o nome; assume-se que a nova playlist já é criada **contendo a música clicada** (coerente com estar dentro de "Adicionar à playlist"). Validar.
- **Premissa P2 — rótulo:** a demanda diz "nova"; adotado **"Nova…"** (reticências indicam que abre diálogo). Validar se deve ser exatamente "Nova".
- **Premissa P3 — duplicado:** assume-se bloquear (não sobrescrever nem adicionar à existente). Alternativa: oferecer "adicionar à existente". Validar.
- **Premissa P4 — aviso "Nenhuma playlist criada":** mantido abaixo do separador quando não há playlists. Alternativa: removê-lo, já que "Nova…" torna o menu acionável.
