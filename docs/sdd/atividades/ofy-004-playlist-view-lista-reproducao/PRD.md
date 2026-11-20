# PRD — ofy-004 — Lista de reprodução (Playlist view)

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Exibe a lista de músicas carregada (pasta, playlist virtual ou todos os arquivos) em uma grade **Artista / Música / Tempo**, destacando a faixa em reprodução, com tooltip de tags, colunas redimensionáveis, rodapé de totais e menu de contexto por música.

## Objetivos

- Navegar visualmente pela biblioteca e iniciar a reprodução com um clique.
- Mostrar metadados relevantes sem abrir outras telas (tooltip).
- Oferecer ações rápidas por música: abrir pasta no Explorer, copiar caminho, adicionar a playlist.

## Histórias de Usuário

- Como ouvinte, quero clicar em uma linha para tocar.
- Como ouvinte, quero ver artista, título e duração, e ajustar a largura das colunas.
- Como ouvinte, quero passar o mouse sobre uma música e ver suas tags.
- Como colecionador, quero abrir a pasta do arquivo no Explorer e copiar seu caminho.
- Como organizador, quero adicionar a música a uma playlist existente pelo menu de contexto.

## Funcionalidades Principais

1. **RF-004.1 Grade:** cada linha mostra `artist` (vazio se ausente), `title` (ou nome do arquivo) e `duration_ms` formatado; a linha da faixa corrente tem classe `active`; clique chama `playFile`.
2. **RF-004.2 Colunas redimensionáveis:** arrastar os separadores do cabeçalho; Artista entre **15%** e **55%** (padrão 30%), Tempo entre **40** e **160 px** (padrão 62 px). Não persiste entre sessões.
3. **RF-004.3 Tooltip de tags:** após **1 s** de hover, mostra Música, Artista, Álbum, Ano, Gênero, Faixa, Duração e Bitrate (apenas campos preenchidos); é reposicionado para não sair da janela.
4. **RF-004.4 Rodapé:** `N música(s)` e soma das durações dos itens exibidos (quando > 0).
5. **RF-004.5 Estados:** carregando (`Carregando dados... n/total`, spinner), vazio (`Nenhum arquivo .mp3 encontrado`).
6. **RF-004.6 Menu de contexto (botão direito):** *Abrir pasta no explorer* (`revealItemInDir` do plugin opener), *Copiar caminho* (clipboard) e submenu *Adicionar à playlist* (lista playlists virtuais, atualizadas ao abrir o menu; sem playlists: item desabilitado "Nenhuma playlist criada").
7. **RF-004.7 Adicionar à playlist:** carrega a playlist (`GET /playlist/{name}`); se a música já está nela, informa; senão salva `[...paths, arquivo]` (`POST /playlist`). Mensagem temporária de 4 s.
8. **Critérios de aceite:** (a) clicar toca a faixa; (b) tooltip só aparece com tags em cache; (c) adicionar música duplicada não altera a playlist e informa; (d) larguras respeitam os limites.

## Experiência do Usuário

- Lista com rolagem; cabeçalho fixo com separadores arrastáveis; mensagens flutuantes `playlist-save-msg`; menu de contexto com ícones (MUI) e submenu por hover.
- Tema claro/escuro via variáveis CSS.

## Restrições Técnicas de Alto Nível

- `revealItemInDir` exige o plugin `opener` do Tauri (não funciona no Vite puro; erros são ignorados).
- Depende do `id3Cache` carregado (ver `ofy-003`/`ofy-006`).

## Fora de Escopo

- Ordenação por coluna, reordenação por arrastar, seleção múltipla, edição de tags na lista, remoção de música da playlist pelo menu da lista.

## Questões em Aberto

- `useContextMenuClose` registra listeners globais diretamente durante o render (não é `useEffect`) e descarta a função de limpeza devolvida; com o menu aberto, cada re-render pode adicionar listeners duplicados `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Não há paginação/virtualização: a lista inteira é renderizada.
- Erro ao abrir pasta no Explorer é ignorado silenciosamente.
