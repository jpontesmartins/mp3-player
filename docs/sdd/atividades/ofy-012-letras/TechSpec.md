# Especificação Técnica — ofy-012 — Letras de músicas (busca, cache, edição e remoção)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Módulo `lyrics` (bounded context) com Clean Architecture: `LyricsController → LyricsService → {Id3Codec, LyricsScraper, LyricRepository}`. `CompositeLyricsScraper` (Composite/Strategy) itera `LyricsSource` (`VagalumeSource`, `LetrasMusBrSource`, ambos `AbstractLyricsSource` — Template Method). Persistência em `FileLyricRepository`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Backend: `LyricsController`, `LyricsService`, `CompositeLyricsScraper`, `LetrasMusBrSource`, `VagalumeSource`, `AbstractLyricsSource`, `FileLyricRepository`, `LyricsConfig`, `LyricsProperties`, modelo `Lyric`, `MusicFileNaming` (shared).
- Frontend: `features/lyrics/ui/LyricsPanel.tsx`, `features/lyrics/lib/useLyrics.ts`, `shared/api/lyrics.ts`.
- Fluxo `GET /lyrics`: `LyricsService.get` → `find` (cache) → miss: `Id3Codec.read` → `LyricsScraper.fetch(artist,title)` → para cada fonte habilitada: `findPage` → remove sufixo `traducao.html` → baixa página → `extractLyrics` → 1º não nulo; senão mensagem "Letra não encontrada…" → `LyricRepository.save`.
- Estratégias de `findPage`: Letras: página do artista → URL direta (HTTP 200) → busca no site; Vagalume: URL direta → página do artista → busca. Extração: Letras `div.lyric-original` (`<p>`); Vagalume `div#lyrics` (`wholeText().trim()`).

## Design de Implementação

### Interfaces Principais

```java
public interface LyricsScraper { String fetch(String artist, String title) throws IOException; }
public interface LyricsSource { String getName(); int getPriority(); boolean isEnabled(); String getUserAgent();
    String findPage(String artist, String title) throws IOException; String extractLyrics(Document page); }
public interface LyricRepository { Optional<Lyric> find(String p); void save(Lyric l, MusicFile m); boolean exists(String p); void delete(String p); }
```

### Modelos de Dados

- `Lyric(musicPath, text)`; `LyricsSaveRequest(String path, String text)`.
- Propriedades: `mp3.lyrics.{letras|vagalume}.{enabled, priority, base-url, user-agent, search-path, timeout-connect, timeout-fetch}`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /lyrics/cached?path=` | query | `200` texto / `404` |
| `GET /lyrics?path=` | query | `200` texto (cache ou buscado) / `400 "Erro ao buscar letra: ..."` |
| `POST /lyrics` | `{ "path": "...", "text": "..." }` | `200 "Letra salva"` / `400` |
| `DELETE /lyrics?path=` | query | `200 "Letra removida"` / `400` |

## Pontos de Integração

- letras.mus.br e vagalume.com.br via Jsoup com cabeçalhos de navegador (`User-Agent`, `Accept-Language pt-BR`, `Sec-Fetch-*`, `Referer`). Falhas de rede em uma fonte → tenta a próxima.

## Abordagem de Testes

### Testes Unitários

- `LyricsServiceTest`, `LyricsControllerTest`, `CompositeLyricsScraperTest`, `LetrasMusBrSourceTest`, `FileLyricRepositoryTest`, `LyricTest`. Não há `VagalumeSourceTest` `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Frontend: sem testes de `LyricsPanel`/`useLyrics`.

### Testes de Integração

- Não há (scraping real não é exercitado).

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Letras via letras.mus.br (1.0) → Template Method + Strategy (1.3.1) → correções de download (1.3.1/1.5.2) → segunda fonte (Vagalume, 1.5.2) → botões fixos e rolagem (1.6.0).

### Dependências Técnicas

- Internet; `ofy-006` (tags para artista/título).

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixos `[Scraper]`, `[letras.mus.br]`, `[vagalume.com.br]`, `[Lyrics]`.

## Considerações Técnicas

### Decisões Principais

- Cache como arquivo ao lado do MP3 (portável com a coleção).
- Múltiplas fontes ordenadas por prioridade configurável.

### Riscos Conhecidos

- Fragilidade a mudanças de HTML e bloqueios anti-bot nos sites.
- Aspectos legais/termos de uso do scraping.
- Persistência da mensagem de "não encontrada" (ver Questões em Aberto).
- Escrita de `.txt` na pasta de música (pode ser somente leitura).

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/lyrics/**`, `backend/src/main/java/com/ovelha/fy/shared/domain/util/MusicFileNaming.java`, `backend/src/main/resources/application.properties`
- `frontend/src/features/lyrics/**`, `frontend/src/shared/api/lyrics.ts`
