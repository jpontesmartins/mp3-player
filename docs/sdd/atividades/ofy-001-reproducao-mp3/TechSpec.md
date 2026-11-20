# Especificação Técnica — ofy-001 — Reprodução de MP3

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Frontend envia comandos HTTP via `shared/api/playback.ts` e mantém o estado em `PlayerContext`; `usePlaybackPolling` consulta `GET /playing` a cada segundo. No backend, `PlayerController → PlayerService → PlayerEngine (porta) → JLayerPlayerEngine`, que decodifica em **virtual thread** (um frame por iteração) e controla pausa/seek por flags voláteis e `ReentrantLock`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- **Backend:** `PlayerController` (`/play`, `/pause`, `/resume`, `/stop`, `/seek`, `/playing`), `PlayerService` (validações simples de estado e montagem do mapa de status), `PlayerEngine` (porta), `JLayerPlayerEngine` (implementação; depende de `Id3Codec` para ler tags da faixa).
- **Frontend:** `features/playback/ui/Player.tsx`, `lib/usePlayback.ts` (comandos), `lib/usePlaybackPolling.ts` (estado), `app/providers/PlayerContext.tsx`, `shared/api/playback.ts`, `shared/lib/format.ts`.
- **Fluxo `play`:** `usePlayback.playFile` → (`stop` se outra faixa) → `POST /play` → `JLayerPlayerEngine.play`: `analyzeFile` (lê todos os frames: `sampleRate`, `totalFrames`) → `readId3Tags` → abre `FileInputStream` (se `start>0`, avança frames e reabre o stream no byte correspondente) → cria `Player` → `Thread.startVirtualThread` toca `FRAMES_PER_CHUNK=1` por vez enquanto `playing && !complete` (dorme 1 ms quando pausado).
- **Fluxo `status`:** `status = stopped` quando `currentFilePath == null` ou `!isPlaying()`; senão `paused|playing`, `position = getElapsedMillis()` (relógio `nanoTime` menos tempo pausado), `duration = getTotalMillis()`, `id3 = getId3Tags()`.

## Design de Implementação

### Interfaces Principais

```java
public interface PlayerEngine {
    void play(String filePath) throws IOException;
    void play(String filePath, long startPositionMillis) throws IOException;
    void pause(); void resume(); void seekTo(long positionMillis); void stop();
    boolean isPlaying(); boolean isPaused();
    String getCurrentFilePath(); long getElapsedMillis(); long getTotalMillis();
    Map<String, String> getId3Tags();
}
```
```ts
// shared/api/playback.ts
play(file: string): Promise<boolean>   // POST text/plain
pause(); resume(); stop(): Promise<boolean>
seek(positionMilliseconds: number): Promise<boolean>   // JSON { position }
getStatus(): Promise<PlayingData | null>
```

### Modelos de Dados

- `PlayingData { status: 'playing'|'paused'|'stopped'; file: string; position: number; duration: number; id3?: Id3Tags }` (ms).
- `PlayerState` no frontend: `currentFile, status, position, duration, playbackMode, showCover, intentionalStop, playHistory`.
- Constantes do motor: `SAMPLES_PER_FRAME = 1152`, `FRAMES_PER_CHUNK = 1`; sample rate padrão 44100 se a análise falhar.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `POST /play` | corpo texto = caminho | `200 "Playing: <caminho>"`; `400 "Error: <msg>"` |
| `POST /pause` | — | `200 "Paused"`; `400 "No music playing"` |
| `POST /resume` | — | `200 "Resumed"`; `400 "No music playing"` / `"Music is not paused"` |
| `POST /stop` | — | `200 "Stopped"` |
| `POST /seek` | `{ "position": <ms> }` | `200 "Seeked to <ms>"`; `400 "Missing position"` / `"No music playing"` |
| `GET /playing` | — | `{status, file, position, duration, id3}`; parado: `{status:"stopped", file:""}` |

## Pontos de Integração

- JLayer (decodificação e saída de áudio) e `Id3Codec` (tags da faixa). Sem integrações de rede.

## Abordagem de Testes

### Testes Unitários

- `PlayerServiceTest`, `PlayerControllerTest` (mocks da porta), `JLayerPlayerEngineTest` (engine com arquivos de teste/mocks de `Id3Codec`).
- Frontend: `Player.test.tsx`, `usePlayback.test.tsx`, `usePlaybackPolling.test.tsx` (mock de `shared/api/playback`).
- Cenários críticos: pausa/retomada, seek, play de arquivo inexistente, parada intencional sem autoplay.

### Testes de Integração

- `Mp3PlayerApplicationTests` valida a injeção de `PlayerEngine`/`PlayerService`. Não há teste com áudio real.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Implementado desde a v0.9 (motor + controller), seguido de seek, correção de sobreposição de faixas (v1.5.3) e de seek para a próxima música (v1.5.4). Para novas mudanças: (1) especificar contrato, (2) ajustar `PlayerEngine`/`PlayerService`, (3) `shared/api`, (4) hooks/UI, (5) testes.

### Dependências Técnicas

- JDK 21 (virtual threads), JLayer 1.0.1, dispositivo de áudio disponível na máquina.

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixo `[Player]` (play, pausa, retomada, seek, falhas de análise/reprodução).

## Considerações Técnicas

### Decisões Principais

- Áudio no backend Java em vez de `<audio>` na WebView: aproveita JLayer e permite ler arquivos locais sem expor o FS ao frontend; custo: seek = reiniciar a decodificação e polling para estado.
- Estado de reprodução exposto por polling (simplicidade) em vez de push.
- Alternativas rejeitadas: não documentadas no código.

### Riscos Conhecidos

- `analyzeFile` percorre o arquivo inteiro a cada `play`/`seek` (custo proporcional ao tamanho).
- Estado compartilhado em campos `volatile` + lock apenas em `play`/`stop`; `pause`/`resume` não usam o lock `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Exceções de decodificação são apenas logadas (`WARN`); o motor marca `playing = false` e o frontend interpreta como fim de faixa (pode disparar autoplay).
- Duração é estimativa por frames (VBR/arquivos com frames inválidos podem divergir).

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/player/controls/{web/PlayerController.java, application/PlayerService.java, infrastructure/JLayerPlayerEngine.java}`
- `backend/src/main/java/com/ovelha/fy/player/domain/port/PlayerEngine.java`
- `frontend/src/features/playback/{ui/Player.tsx, lib/usePlayback.ts, lib/usePlaybackPolling.ts}`
- `frontend/src/app/providers/PlayerContext.tsx`, `frontend/src/shared/api/playback.ts`, `frontend/src/shared/lib/format.ts`
- Testes: `backend/src/test/java/com/ovelha/fy/player/controls/**`, `frontend/src/features/playback/__tests__/**`
