# PRD — ofy-002 — Navegação entre faixas, modos de reprodução e autoplay

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Define como o player escolhe a próxima/anterior faixa e o que acontece quando uma música termina. Há três modos de reprodução — **Contínua**, **Aleatória** e **Repetição** — e um histórico de faixas tocadas para o botão "Anterior". Toda a lógica está no frontend.

## Objetivos

- Tocar automaticamente a próxima faixa ao fim da atual, respeitando o modo escolhido.
- Permitir pular para a próxima e voltar para a anterior manualmente.
- Não avançar automaticamente quando o usuário clicou em Parar.

## Histórias de Usuário

- Como ouvinte, quero que a próxima música toque sozinha para ouvir o álbum inteiro.
- Como ouvinte, quero o modo aleatório para variar a ordem.
- Como ouvinte, quero repetir a faixa atual.
- Como ouvinte, quero voltar à música tocada anteriormente.
- Caso extremo: playlist vazia → nada acontece; usuário pressiona Parar → não avança.

## Funcionalidades Principais

1. **RF-002.1 Próxima (`next`)**: usa `getNextFile(currentFile, playlistFiles, mode)`:
   - `repeat` → devolve a própria faixa corrente;
   - `shuffle` → faixa aleatória da lista (pode repetir a mesma);
   - `continuous` → próxima da lista; se sem faixa atual, a primeira; se for a última (ou não encontrada), volta à primeira.
2. **RF-002.2 Anterior (`previous`)**: volta à última faixa do `playHistory` (e a remove do histórico). Sem histórico, não faz nada.
3. **RF-002.3 Histórico**: ao tocar uma faixa diferente da corrente via `playFile`, a faixa anterior é empilhada (máximo **100**).
4. **RF-002.4 Autoplay**: quando o backend reporta `stopped` e o estado anterior no frontend era `playing`/`paused`, sem parada intencional e sem avanço em curso, o polling toca a próxima faixa conforme o modo.
5. **RF-002.5 Parar intencional**: o botão Parar marca `intentionalStop`; o próximo polling `stopped` não avança e limpa a marca.
6. **RF-002.6 Critérios de aceite**: (a) fim de faixa em modo contínuo toca a seguinte; (b) na última faixa volta à primeira; (c) em `repeat` a mesma faixa recomeça; (d) após Parar não há autoplay; (e) dois `stopped` seguidos não disparam dois avanços (guarda `advancingRef`).

## Experiência do Usuário

- Botões Anterior/Próxima do Player ficam habilitados quando há faixa corrente.
- O modo é escolhido em Configurações → "Tipo de reprodução" (ver `ofy-014`); não há indicador do modo no Player.

## Restrições Técnicas de Alto Nível

- Depende do polling de `GET /playing` (1 s): o avanço ocorre em até ~1 s após o fim da faixa.
- A lista usada é `LibraryContext.playlistFiles` (pasta carregada, playlist virtual ou "todos os arquivos").

## Fora de Escopo

- Parar ao fim da lista (modo "stop at end"): o README cita "além de stop" como comando, mas não há modo que encerre ao fim da playlist.
- Fila de reprodução, "tocar a seguir", shuffle sem repetição.

## Questões em Aberto

- `getPreviousFile` existe e é testado (`navigation.test.ts`) mas **não é usado** pela aplicação; "Anterior" usa apenas `playHistory` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- O autoplay (polling) toca via `playbackApi.play` direto e **não registra histórico**; apenas `playFile` registra `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- A busca (filtro) da playlist **não** altera `playlistFiles`: Próxima/autoplay percorrem a lista completa, não o resultado filtrado `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Em `repeat`, o botão "Próxima" replica a mesma faixa (comportamento por `getNextFile`).
