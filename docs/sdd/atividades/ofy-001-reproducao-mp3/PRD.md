# PRD — ofy-001 — Reprodução de MP3

> Levantamento retroativo (v1.6.0): descreve o comportamento **já implementado** no código. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Visão Geral

Permite tocar arquivos MP3 locais com controles de mídia (tocar, pausar, retomar, parar e buscar posição) e acompanhar tempo decorrido/duração e a faixa atual. A reprodução de áudio acontece no **backend Java** (JLayer); o frontend apenas envia comandos e consulta o estado.

## Objetivos

- Tocar qualquer `.mp3` da biblioteca ao clicar na lista.
- Permitir pausar/retomar, parar e saltar para uma posição clicando na barra de progresso.
- Refletir no frontend o estado real do motor de áudio (posição, duração, tags da faixa) a cada 1 s.
- Evitar sobreposição de faixas ao trocar de música (a faixa anterior é parada antes).

## Histórias de Usuário

- Como ouvinte, quero clicar numa música da lista para ela começar a tocar.
- Como ouvinte, quero pausar e retomar de onde parei.
- Como ouvinte, quero parar a reprodução e voltar a posição para zero.
- Como ouvinte, quero clicar na barra de progresso para avançar/voltar na música.
- Caso extremo: arquivo inexistente/ilegível → a reprodução falha (HTTP 400) e o estado do player não muda.

## Funcionalidades Principais

1. **RF-001.1 Tocar:** `POST /play` com o caminho como corpo `text/plain`. Se já havia outra faixa, o frontend chama `/stop` antes de tocar a nova.
2. **RF-001.2 Pausar/Retomar:** `POST /pause` e `POST /resume`; só válidos quando há música tocando (e, para retomar, pausada); caso contrário o backend responde 400 e o frontend não altera o estado.
3. **RF-001.3 Parar:** `POST /stop` encerra a reprodução e limpa a faixa corrente do motor.
4. **RF-001.4 Buscar posição (seek):** `POST /seek {position: ms}` reinicia a reprodução na posição pedida; o frontend só permite clicar na barra após **2 s** do início da faixa e quando a duração é conhecida.
5. **RF-001.5 Estado:** `GET /playing` devolve `status` (`playing`/`paused`/`stopped`), `file`, `position`, `duration` (ms) e `id3`; o frontend consulta a cada 1 s.
6. **RF-001.6 Exibição:** botão play/pause alterna ícone; stop só habilitado em `playing`/`paused`; tempo no formato `HH:MM:SS / HH:MM:SS` (duração desconhecida: `--:--:--`); texto de status: nome da faixa (tags `artista - título` ou nome do arquivo), `Pausado: …` ou `Nenhuma música tocando`; clicar no texto de status rola a lista até a faixa ativa.
7. **RF-001.7 Critérios de aceite:** (a) clicar numa música inexistente no estado atual não deixa duas faixas tocando; (b) após pausar, `GET /playing` retorna `paused` e a posição não avança; (c) após `seek`, a posição reportada parte do valor pedido; (d) `stop` → `status: "stopped"`.

## Experiência do Usuário

- Painel direito → seção Player: botões Anterior, Play/Pause, Parar, Próxima (ícones MUI), barra de progresso clicável (cursor `pointer` ou `not-allowed`), tempo e status.
- Playlist: a linha da faixa corrente recebe a classe `active`.
- Acessibilidade: botões nativos; sem rótulos ARIA dedicados `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

## Restrições Técnicas de Alto Nível

- Apenas MP3 (JLayer 1.0.1). Saída de áudio pelo dispositivo padrão do SO via Java Sound (módulo `java.desktop` presente no JRE `jlink`).
- Backend em `localhost:8111`; sem WebSocket (polling 1 s).
- Duração calculada por contagem de frames (`frames × 1152 / sampleRate`).

## Fora de Escopo

- Controle de volume, mute, equalizador, crossfade, gapless.
- Fila de reprodução independente da playlist carregada.
- Pré-visualização/waveform.

## Questões em Aberto

- Ao parar, o frontend define `status = 'paused'` e `position = 0` imediatamente e só depois o polling corrige para `stopped` `[PENDENTE / AMBÍGUO NO CÓDIGO]` (efeito visual transitório de até ~1 s).
- Seek durante pausa: o motor reinicia tocando (`paused = false`); o frontend só reflete `playing` no próximo polling `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- `isSeekAllowed` é calculado a cada render a partir de `Date.now()`; se nada re-renderiza após 2 s a barra pode continuar bloqueada até o próximo polling (que atualiza a posição) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
