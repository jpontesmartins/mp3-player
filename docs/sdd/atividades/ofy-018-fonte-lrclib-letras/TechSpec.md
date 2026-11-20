# Especificação Técnica — ofy-018 — Nova fonte de letras (LRCLIB)

> Evolução de `ofy-012`/`ofy-017`. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

As fontes atuais (`LyricsSource`) são **baseadas em página HTML** (`findPage` → URL, `extractLyrics(Document Jsoup)`), um contrato que não serve a uma API JSON. Esta atividade introduz uma segunda porta de domínio, **`LyricsApiSource`** (sem Jsoup), e a implementação `LrclibSource` (infrastructure) sobre o novo cliente `LrclibClient` (`java.net.http.HttpClient` + Jackson, ambos já disponíveis). O `CompositeLyricsScraper` passa a consultar primeiro as fontes de página (Vagalume, letras.mus.br — comportamento atual) e, **somente se nada for encontrado**, as fontes de API por prioridade.

Para entregar a consulta exata e a escolha da gravação correta, o contrato do scraper evolui de `fetch(List<String> artists, String title)` (ofy-017) para `fetch(LyricsQuery)`, onde `LyricsQuery(artistCandidates, title, album, durationMs)` agrega o que o `LyricsService` já lê das tags. Regras puras (variações de título, escolha entre resultados) ficam em classes sem I/O, testáveis sem rede. Os contratos HTTP de `/lyrics` e o formato do `.txt` não mudam.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `LyricsQuery` (novo, `lyrics/domain/model`, `record`): `List<String> artistCandidates`, `String title`, `String album` (nullable), `Long durationMs` (nullable). Sem dependência de framework.
- `TitleVariants` (novo, `lyrics/domain/model`, utilitária pura): `List<String> from(String title)` conforme RF-018.4.
- `LyricsApiSource` (nova porta, `lyrics/domain/port`): `getName()`, `getPriority()`, `isEnabled()`, `Optional<String> find(LyricsQuery query)`.
- `LyricsScraper` (porta, alterada): `Optional<String> fetch(LyricsQuery query)`.
- `LyricsService` (application): monta `LyricsQuery` (artistas via `ArtistCandidates`, título/álbum/duração das tags) e chama o scraper.
- `CompositeLyricsScraper` (infrastructure, alterado): recebe `List<LyricsSource>` **e** `List<LyricsApiSource>`; fluxo: fontes de página × candidatos (inalterado) → se vazio, fontes de API por prioridade → `Optional.empty()`.
- `LrclibSource` (novo, infrastructure, implementa `LyricsApiSource`): orquestra consulta exata → busca por faixa → busca por título, aplicando `TitleVariants` e `LrclibResultPicker`.
- `LrclibResultPicker` (novo, infrastructure, puro): escolhe o melhor resultado conforme RF-018.3.
- `LrclibClient` (novo, infrastructure): `Optional<JsonNode> get(String path, Map<String,String> params)` e `List<JsonNode> search(Map<String,String> params)`; `404` → vazio; erros/timeouts/JSON inválido → `WARN` e vazio.
- `LyricsConfig` / `LyricsProperties`: novo bean `List<LyricsApiSource>` e record `Lrclib` em `mp3.lyrics.lrclib.*`.
- Fluxo `GET /lyrics` (miss): `LyricsService.get` → `Id3Codec.read` → `LyricsQuery` → `CompositeLyricsScraper.fetch` → [Vagalume, letras.mus.br × candidatos] → vazio → [`LrclibSource.find`] → `Optional` → `save` (se presente) / mensagem sem persistir (ofy-017).

Algoritmo de `LrclibSource.find(query)` (para o título e cada variante de `TitleVariants.from(title)`, e cada artista candidato, **parando no primeiro acerto**; limite total de **6 requisições** por música):
1. `GET /api/get?track_name=<t>&artist_name=<a>[&album_name=<alb>][&duration=<segundos inteiros>]` → `200` com `plainLyrics` útil → retorna.
2. `GET /api/search?track_name=<t>&artist_name=<a>` → `LrclibResultPicker` → retorna se houver.
3. Após esgotar artistas/variantes: `GET /api/search?track_name=<t>` (título original e a 1ª variante diferente) → `LrclibResultPicker` com o artista principal → retorna ou vazio.
Se o limite de 6 requisições for atingido, encerra com vazio (log `WARN`).

## Design de Implementação

### Interfaces Principais

```java
public record LyricsQuery(List<String> artistCandidates, String title, String album, Long durationMs) {}

public interface LyricsScraper {
    Optional<String> fetch(LyricsQuery query) throws IOException;
}

public interface LyricsApiSource {
    String getName(); int getPriority(); boolean isEnabled();
    Optional<String> find(LyricsQuery query);   // sem exceções de rede: falhas viram Optional.empty()
}

public final class TitleVariants { public static List<String> from(String title); }
```

Regras de `TitleVariants.from` (ordem, sem vazios/duplicatas; teto de 4 variantes):
1. título original (`strip`);
2. sem `?` e `!`, espaços colapsados — `O Que Será? (À Flor Da Pele)` → `O Que Será (À Flor Da Pele)`;
3. 2 sem o grupo final entre `()`/`[]` — → `O Que Será`;
4. 3 sem sufixo `\s+-\s+(\d{4}\s+)?(Remaster(ed)?|Ao Vivo|Live)\b.*$` (case-insensitive).
Nunca devolve string vazia; se uma remoção esvaziar o título, a variante é descartada.

`LrclibResultPicker.pick(List<LrclibTrack> results, String artist, String title, Long durationMs)` — `LrclibTrack` = record interno `(trackName, artistName, albumName, duration, instrumental, plainLyrics)`. Filtra `plainLyrics` não vazio e `instrumental != true`; compara artista/título sem acento e sem caixa (`Normalizer` NFD + `\p{M}` removido); tolerância de duração ±5 s; prioridade (a) artista+duração, (b) artista, (c) título normalizado idêntico+duração, (d) título normalizado idêntico; empate → primeiro da lista (ordem da API).

### Modelos de Dados

- Resposta LRCLIB usada (campos): `trackName`, `artistName`, `albumName`, `duration` (segundos, decimal), `instrumental` (boolean), `plainLyrics` (string/null). Demais campos (`syncedLyrics`, `lyricsfile`, `id`…) ignorados (`@JsonIgnoreProperties(ignoreUnknown=true)` ou leitura via `JsonNode`).
- Sem mudança de modelo persistido: `Lyric(musicPath, text)` inalterado; `.txt` inalterado.
- Propriedades novas (`LyricsProperties.Lrclib`): `enabled` (padrão `true`), `priority` (`3`), `baseUrl` (`https://lrclib.net`), `userAgent` (`ovelhafy-mp3-player (lrclib client)`), `timeoutConnect` (`8000`), `timeoutFetch` (`15000`).

### Endpoints de API

- **Backend do app:** sem alteração (`GET /lyrics`, `GET /lyrics/cached`, `POST /lyrics`, `DELETE /lyrics`).
- **Externos consumidos (LRCLIB):**

| Chamada | Uso | Resposta |
|---|---|---|
| `GET {base}/api/get?track_name&artist_name[&album_name][&duration]` | consulta exata | `200` objeto / `404` |
| `GET {base}/api/search?track_name[&artist_name]` | busca | `200` array (≤ ~20) |

Parâmetros codificados com `URLEncoder` (UTF-8); `duration` em segundos inteiros arredondados de `durationMs`.

## Pontos de Integração

- **LRCLIB (`lrclib.net`)**, serviço público sem autenticação. Cabeçalhos: `User-Agent` configurável (identifica o app, sem dados pessoais), `Accept: application/json`.
- Tratamento de erro: `404` em `/api/get` → sem resultado (não é erro); `429`/`5xx`/timeout/`IOException`/JSON inválido → `WARN [lrclib.net] <causa>` e `Optional.empty()`; nenhuma exceção sai de `LrclibSource` (a busca das demais fontes e a UI não são afetadas). Sem retry.
- Privacidade: apenas título, artista, álbum e duração são enviados; **nunca** caminho do arquivo nem nome de pasta.

## Abordagem de Testes

### Testes Unitários

AAA, JUnit 5 + Mockito, **sem rede**.

- `TitleVariantsTest` (novo): `O Que Será? (À Flor Da Pele)` → `[original, sem ?, "O Que Será"]`; `Song - 2011 Remastered` e `Song - Ao Vivo`; título sem sufixos → 1 variante; título só entre parênteses não gera variante vazia; sem duplicatas.
- `LrclibResultPickerTest` (novo): preferência (a)>(b)>(c)>(d); descarta `instrumental=true` e `plainLyrics` vazio/nulo; tolerância de ±5 s; comparação sem acento/caixa (`Leão, Nara` vs `Nara Leão` não casa por artista exato → cai em (c)/(d), documentado); lista vazia → vazio.
- `LrclibSourceTest` (novo, `LrclibClient` mockado): exata acerta → 1 requisição e nada mais; exata 404 → busca por faixa; só busca por título resolve; teto de 6 requisições; falha do cliente → vazio sem exceção; envia `duration` arredondada e `album_name` quando presentes.
- `LrclibClientTest` (novo, servidor HTTP local `com.sun.net.httpserver.HttpServer` na porta efêmera): `200` JSON, `404`, `500`, JSON inválido, timeout; verifica `User-Agent` e codificação de parâmetros (acentos).
- `CompositeLyricsScraperTest` (alterado): fontes de página acertam → `LyricsApiSource.find` **nunca** chamada (`verify never`); páginas falham → API consultada; API desabilitada não é chamada; API lançando `RuntimeException` não propaga.
- `LyricsServiceTest` (alterado): monta `LyricsQuery` com álbum/duração das tags (`durationMs` pode ser `null`); falha total → mensagem sem `save`.
- `LyricsConfig`/contexto: `Mp3PlayerApplicationTests` (injeção dos novos beans, propriedades padrão presentes).

### Testes de Integração

- Manual com rede (fora do CI): as faixas dos critérios (a) e (b) e uma música do Vagalume (confirmar ausência de chamada ao LRCLIB pelo log). Registrar resultado em `referencias.md` na implementação.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

1. `LyricsQuery` + evolução da porta `LyricsScraper`/`LyricsService`/`CompositeLyricsScraper` (refator sem comportamento novo; testes atuais adaptados).
2. `TitleVariants` + testes.
3. `LrclibResultPicker` + testes.
4. `LrclibClient` + testes com servidor local.
5. `LyricsApiSource` + `LrclibSource` + testes.
6. Integração no `CompositeLyricsScraper` (fontes de API após as de página), `LyricsProperties`/`LyricsConfig`/`application.properties`.
7. `mvn test`; validação manual; atualizar `docs/sdd/TechSpec.md` (integrações externas, riscos), `Rules.md` §1/§2 (lista de externos: adicionar `lrclib.net`) e `ofy-012` (fontes, ordem, propriedades).

### Dependências Técnicas

- Internet e disponibilidade do LRCLIB (apenas validação manual). Sem dependências Maven novas (`java.net.http` do JDK 21; Jackson já presente via `spring-boot-starter-web`). **Depende da `ofy-017`** (`ArtistCandidates`, `Optional` no scraper, não persistir falha).

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs com prefixo `[lrclib.net]`: `INFO` ao iniciar consulta (URL sem dados além de título/artista), `INFO` ao escolher resultado (artista/álbum/regra a-d), `WARN` em falhas (`status`, timeout, JSON inválido) ou teto de requisições; `[Scraper] Fontes habilitadas` passa a listar também as fontes de API.

## Considerações Técnicas

### Decisões Principais

- **Nova porta `LyricsApiSource` em vez de adaptar `LyricsSource`:** `LyricsSource` expõe `org.jsoup.nodes.Document` (exceção já marcada como pendência em `Rules.md` §2, "não replicar"). Forçar uma API JSON nesse contrato exigiria um `Document` falso. Alternativa rejeitada: generalizar `LyricsSource` (quebraria `LetrasMusBrSource`, `VagalumeSource` e seus testes sem ganho).
- **`LyricsQuery` em vez de mais parâmetros na porta:** álbum e duração só interessam às fontes de API; o record evita nova mudança de assinatura quando surgirem outros campos. Custo: segunda mudança da porta `LyricsScraper` (ofy-017 → ofy-018), com ajuste dos testes.
- **Fontes de página primeiro, API por último:** preserva o comportamento e a latência atuais (critério (c)) e limita chamadas ao serviço externo.
- **`HttpClient` do JDK + Jackson, não Jsoup:** a resposta é JSON; evita `ignoreContentType` e o parse HTML inútil. Alternativa rejeitada: cliente Spring (`RestClient`) — válido, mas acrescenta acoplamento ao Spring na `infrastructure` sem necessidade; o `HttpClient` é suficiente e fácil de testar com `HttpServer` local.
- **Somente `plainLyrics`:** consistente com o `.txt` e a UI atuais; LRC está fora de escopo.
- **Escolha pela gravação correta (artista + duração) com *fallback* para mesma música de outro intérprete:** aumenta a cobertura (caso *O Que Será*); risco de letra de versão diferente documentado.
- **Teto de 6 requisições por música:** evita explosão (artistas × variantes) e proteção do serviço público.

### Riscos Conhecidos

- **Termos de uso/disponibilidade do LRCLIB** (serviço comunitário): mitigação — fonte desabilitável (`enabled=false`), falhas silenciosas, sem retry.
- **Letra de outro intérprete/versão** (regra (d)): mitigação — preferências (a)-(c), log da regra aplicada; usuário pode `Alterar`/`Remover letra`.
- **Qualidade/legalidade do conteúdo** das letras de terceiros: igual ao risco já existente do scraping (ver `ofy-012`).
- **Normalização de título agressiva** pode casar música errada (ex.: título genérico "Intro"): mitigação — a variante só é usada se artista bater (a/b) ou título normalizado for idêntico (c/d) e há teto de requisições; título de uma palavra sem artista confirmado não é aceito pela regra (d) quando houver `artistCandidates` e a duração divergir mais de 5 s `[PENDENTE / AMBÍGUO NO CÓDIGO]` — definir na implementação, cobrir com teste.
- **Mudança dupla da porta `LyricsScraper`:** mitigação — passo 1 da ordem de construção isola o refator.
- **Latência extra em falha total** (até 6 requisições × 15 s no pior caso): mitigação — timeouts configuráveis; considerar teto global de tempo (ex.: 20 s) `[PENDENTE / AMBÍGUO NO CÓDIGO]`.

### Conformidade com Padrões

- `docs/sdd/Rules.md`: §2 (porta em `domain/port` sem Jsoup/Spring; `application` só depende de portas; implementações em `infrastructure`), §3 (Ports & Adapters, injeção por construtor, `LrclibClient` como adaptador, logs com prefixo, Javadoc, propriedades via `@ConfigurationProperties`), §4 (teste unitário para toda classe nova/alterada, AAA, mocks só de I/O externo), §5 (atualização de `TechSpec.md` macro/`Rules.md` ao concluir), §6 (nova fonte **somente com spec**; sem credenciais; timeouts configurados).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md`.
- (A pasta `.cursor/rules` do template não existe neste repositório.)

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/lyrics/domain/model/{LyricsQuery,TitleVariants}.java` (novos)
- `backend/src/main/java/com/ovelha/fy/lyrics/domain/port/{LyricsScraper,LyricsApiSource}.java`
- `backend/src/main/java/com/ovelha/fy/lyrics/application/LyricsService.java`
- `backend/src/main/java/com/ovelha/fy/lyrics/infrastructure/{CompositeLyricsScraper,LyricsConfig,LyricsProperties}.java` e novos `LrclibClient`, `LrclibSource`, `LrclibResultPicker`
- `backend/src/main/resources/application.properties`
- Testes: `backend/src/test/java/com/ovelha/fy/lyrics/**` (novos `TitleVariantsTest`, `LrclibResultPickerTest`, `LrclibSourceTest`, `LrclibClientTest`)
- Docs a atualizar ao final: `docs/sdd/TechSpec.md`, `docs/sdd/Rules.md` (§1/§2), `docs/sdd/atividades/ofy-012-letras/*`
