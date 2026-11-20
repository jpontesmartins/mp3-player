# Especificação Técnica — ofy-002 — Navegação entre faixas, modos de reprodução e autoplay

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Lógica pura em `navigation.ts` + hooks `usePlayback` (ações manuais) e `usePlaybackPolling` (autoplay). Estado em `PlayerContext` (com refs `currentFileRef`, `modeRef`, `playlistRef`, `playHistoryRef` para uso dentro do laço assíncrono). Nenhum endpoint novo: usa `/play` e `/playing` (ver `ofy-001`).

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `features/playback/lib/navigation.ts`: `getNextFile`, `getPreviousFile`.
- `features/playback/lib/usePlayback.ts`: `next`, `previous`, `playFile`, `stop` (seta `intentionalStop`).
- `features/playback/lib/usePlaybackPolling.ts`: laço `while(!cancelled)` com `delay(1000)`; ao ver `stopped` com estado anterior ativo chama `getNextFile` e `playbackApi.play`; também atualiza `id3Cache` com as tags do status e escreve as tags no `console.log` ao trocar de faixa.
- `app/App.tsx`: instancia `usePlaybackPolling()` uma única vez.
- Fluxo autoplay: fim da faixa → backend `playing=false` → `GET /playing` retorna `stopped` → frontend: `setStatus('stopped')`, zera posição/duração → se elegível, `advancingRef=true` → `play(next)` → `setCurrentFile/Status('playing')` → `advancingRef=false` → `setIntentionalStop(false)`.

## Design de Implementação

### Interfaces Principais

```ts
export function getNextFile(current: string | null, files: string[], mode: PlaybackMode): string | null
export function getPreviousFile(current: string | null, files: string[], mode: PlaybackMode): string | null // não usado em runtime
type PlaybackMode = 'continuous' | 'shuffle' | 'repeat'
```

### Modelos de Dados

- `PlaybackMode`, `Status`, `playHistory: string[]` (≤ 100), `intentionalStop: boolean`.

### Endpoints de API

Reutiliza `POST /play`, `POST /stop` e `GET /playing` (ver `ofy-001`). Nenhum endpoint exclusivo.

## Pontos de Integração

- Não se aplica (somente backend local via API existente).

## Abordagem de Testes

### Testes Unitários

- `navigation.test.ts` (modos, bordas), `usePlayback.test.tsx`, `usePlaybackPolling.test.tsx` (avanço, parada intencional, guarda de duplicidade).
- Mocks: `shared/api/playback` e contextos.

### Testes de Integração

- Não há testes de integração frontend↔backend.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Controles básicos (1.0) → modos e autoplay (1.0–1.1) → correção de autoplay (1.5.1) e controles stop/anterior (1.5.0) → correção de músicas sobrepostas (1.5.3).

### Dependências Técnicas

- `ofy-001` (API de reprodução) e `ofy-003` (lista `playlistFiles`).

## Monitoramento e Observabilidade

- Sem telemetria. Erros de rede são silenciosos no cliente HTTP (`shared/api/client.ts`); diagnóstico via logs do backend e `console` da WebView.

## Considerações Técnicas

### Decisões Principais

- Regras de navegação no frontend, pois o backend não conhece a playlist corrente.
- Polling único para estado + autoplay, com refs para evitar *stale closures*.

### Riscos Conhecidos

- Condição de corrida entre clique manual e avanço automático (mitigada por `advancingRef` e `intentionalStop`, sem teste E2E).
- Se o backend ficar indisponível (`getStatus` retorna `null`), o laço apenas espera 1 s e tenta novamente.
- Falha de reprodução da faixa (`play` retorna `false`) no autoplay não tenta a próxima; o laço continua em `stopped`.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/frontend-style/SKILL.md` (visual monocromático, Feature-Sliced Design, TS strict).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `frontend/src/features/playback/lib/{navigation.ts, usePlayback.ts, usePlaybackPolling.ts}`
- `frontend/src/app/providers/PlayerContext.tsx`, `frontend/src/app/App.tsx`
- Testes: `frontend/src/features/playback/__tests__/lib/*.test.ts(x)`
