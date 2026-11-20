# Especificação Técnica — ofy-017 — Bugfix: falha no download de letras

> Atividade de correção sobre `ofy-012`. Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

Quatro defeitos independentes no módulo `lyrics` (detalhados em `referencias.md`) fazem a busca falhar para músicas com acento ou cujo artista da tag difere do artista do site: (1) `AbstractLyricsSource.toSlug` preserva diacríticos; (2) `LyricsService` usa **um único** artista (tag, ou nome do arquivo só se a tag estiver vazia); (3) as buscas internas dos sites estão quebradas e uma falha HTTP delas aborta a fonte inteira; (4) `LyricsService.get` persiste a mensagem "Letra não encontrada…" como se fosse letra.

Solução: normalizar o slug para ASCII; introduzir **artistas candidatos** (tag + artistas inferidos do nome do arquivo) propagados do `LyricsService` ao `CompositeLyricsScraper` por meio de uma evolução da porta `LyricsScraper`; o scraper passa a sinalizar "não encontrada" com `Optional.empty()` em vez de devolver texto, e o serviço monta a mensagem amigável **sem persisti-la**; textos legados "não encontrada" no cache são tratados como ausência. Contratos HTTP de `/lyrics` e formato do `.txt` permanecem inalterados.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- `LyricsService` (application): resolve artista/título, monta a lista de candidatos, chama o scraper, monta a mensagem de falha, decide o que persistir e ignora cache legado.
- `ArtistCandidates` (novo, `lyrics/domain/model`, classe utilitária pura): `List<String> from(String tagArtist, String musicPath)` — gera candidatos ordenados e sem duplicatas (comparação por slug). Sem dependência de framework.
- `Lyric` (domain): ganha `NOT_FOUND_PREFIX`, `notFoundMessage(artist,title)` e `isNotFoundMessage(text)`.
- `LyricsScraper` (porta): nova assinatura `Optional<String> fetch(List<String> artistCandidates, String title)`.
- `CompositeLyricsScraper` (infrastructure): para cada fonte, para cada candidato, executa o fluxo atual; primeiro resultado não nulo vence; devolve `Optional.empty()` se nada encontrado.
- `AbstractLyricsSource` (infrastructure): `toSlug` com remoção de diacríticos.
- `LetrasMusBrSource` / `VagalumeSource`: `findViaSearch` passa a capturar `IOException` (log `WARN`, retorna `null`).
- Fluxo `GET /lyrics`: `getCached` (ignora cache legado) → miss: `Id3Codec.read` → `ArtistCandidates.from(tag, path)` → `LyricsScraper.fetch(candidatos, título)` → `Optional` presente: `save` + retorna texto; vazio: retorna `Lyric.notFoundMessage(...)` **sem** `save`.

## Design de Implementação

### Interfaces Principais

```java
public interface LyricsScraper {
    /** @return letra, ou vazio se nenhuma fonte/candidato a encontrou */
    Optional<String> fetch(List<String> artistCandidates, String title) throws IOException;
}

public final class ArtistCandidates {
    /** tag primeiro; depois artistas do nome do arquivo; sem vazios/duplicatas (por slug). */
    public static List<String> from(String tagArtist, String musicPath);
}
// Lyric
public static final String NOT_FOUND_PREFIX = "Letra não encontrada para ";
public static String notFoundMessage(String artist, String title);   // formato atual preservado
public static boolean isNotFoundMessage(String text);                // text.strip() começa com prefixo e é uma única linha
```

Regras de `ArtistCandidates.from`:
1. Candidato 1: `tagArtist` (trim), se não vazio.
2. Segmento de artista do nome do arquivo: `MusicFileNaming.artistFromFilename(path)`; remover prefixo de faixa por regex `^\d+([-.]\d+)?\s*[-.]?\s*` (ex.: `1-05 `, `04 `); incluir o segmento completo e, se divergente, cada parte da divisão por `\s*,\s*|\s+e\s+|\s+&\s+|\s+(feat\.?|ft\.?|part\.?)\s+|\s+x\s+` (case-insensitive).
3. Deduplicar por `AbstractLyricsSource`-slug equivalente (comparar em minúsculas sem acentos) para não repetir `gilberto gil` / `Gilberto Gil`.
4. Ordem das partes: preservar a ordem de aparição; a tag sempre primeiro. Para `1-11 Gilberto Gil e Os Mutantes - Bat Macumba` + tag `Gilberto Gil` → `["Gilberto Gil", "Os Mutantes"]` (o segmento completo `Gilberto Gil e Os Mutantes` não gera URL válida e custa requisições: **omitir o segmento composto quando houver divisão**, usar só as partes).
5. Limite de 4 candidatos (proteção contra explosão de requisições).

`toSlug` novo:

```java
String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
return n.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").trim().replaceAll("[\\s-]+", "-");
```

(o `-` literal passa a ser preservado/colapsado; antes era removido — ex.: `Jay-Z` → `jay-z`, que é o slug real dos sites. Impacto nas demais strings: nenhum para nomes sem hífen.) `[PENDENTE / AMBÍGUO NO CÓDIGO]`: confirmar em execução que `Jay-Z`/`AC/DC` geram slugs corretos (`ac-dc` vs `acdc`) — fora do escopo desta atividade, mas registrar no log de teste.

### Modelos de Dados

- Sem mudança de modelo persistido. `Lyric(musicPath, text)` inalterado; apenas métodos estáticos novos.
- `LyricsSaveRequest(path, text)` inalterado.
- Persistência: `.txt` legado com mensagem de falha é lido por `FileLyricRepository.find` normalmente; a filtragem ocorre no `LyricsService` (regra de negócio), **não** no repositório. `FileLyricRepository.exists` continua refletindo a existência do arquivo `[PENDENTE / AMBÍGUO NO CÓDIGO]` (usado apenas em testes/porta; verificar usos antes de alterar).

### Endpoints de API

| Método/caminho | Mudança | Resposta |
|---|---|---|
| `GET /lyrics/cached?path=` | cache legado "não encontrada" → tratado como ausente | `200` texto / `404` |
| `GET /lyrics?path=` | ignora cache legado; falha **não** grava `.txt` | `200` letra \| `200 "Letra não encontrada para \"<título>\" de <artista>"` / `400 "Erro ao buscar letra: ..."` |
| `POST /lyrics`, `DELETE /lyrics` | sem mudança | — |

`<artista>` na mensagem de falha continua sendo o artista principal (tag; fallback nome do arquivo), para manter o texto atual.

## Pontos de Integração

- letras.mus.br e vagalume.com.br (Jsoup, sem autenticação). Observado (ver `referencias.md`): `vagalume.com.br/search.php?q=` → `404`; API `api.vagalume.com.br` → `503`; letras.mus.br responde `403` a clientes não-navegador (`curl`) mas atende o Jsoup do app nas páginas de artista. Tratamento: erros de busca são capturados por estratégia e por candidato; um `IOException` em `findPage`/`connect` de um candidato não impede os demais; a falha só vira `Optional.empty()` ao esgotar tudo.
- Aumento de tráfego: no pior caso `fontes(2) × candidatos(≤4) × tentativas(artistSlugs ≤4 URLs diretas + páginas de artista + busca)`. Mitigações: limite de 4 candidatos; deduplicação; caminho feliz inalterado (primeiro candidato/primeira URL acerta e retorna).

## Abordagem de Testes

### Testes Unitários

Padrão AAA, JUnit 5 + Mockito, sem rede (mockando `Jsoup` como em `LetrasMusBrSourceTest`).

- `ArtistCandidatesTest` (novo): tag + arquivo `1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3` → `[Gilberto Gil, Os Mutantes]`; `1-05 Gilberto Gil, Caetano Veloso, Gal Costa e Os Mutantes - Parque Industrial.mp3` → tag + `Caetano Veloso`, `Gal Costa` (limite 4); sem tag e arquivo `Artista - Título.mp3`; arquivo sem separador → só tag; deduplicação case/acento-insensível; prefixo de faixa removido.
- `LetrasMusBrSourceTest` / novo `VagalumeSourceTest`: `Nara Leão` → URL tentada contém `nara-leao`; `Miserere Nóbis` → `miserere-nobis`; nome sem acento inalterado (`iggy-pop/the-passenger`); `findViaSearch` com `connect` lançando `IOException` retorna `null` sem propagar.
- `CompositeLyricsScraperTest`: itera candidatos (primeiro falha, segundo acerta → `Optional` com letra); todas as fontes/candidatos nulos → `Optional.empty()`; fonte que lança `IOException` não impede a próxima.
- `LyricsServiceTest`: (i) falha → retorna mensagem e **`verify(repo, never()).save`**; (ii) cache legado "não encontrada" → `getCached` devolve `null` e `get` refaz a busca e salva a letra nova; (iii) cache com letra real → devolvida sem chamar scraper; (iv) candidatos passados ao scraper = `ArtistCandidates.from(...)`.
- `LyricTest`: `isNotFoundMessage` verdadeiro para a mensagem exata e variações de espaço; falso para letras contendo a frase no meio de várias linhas e para texto vazio.
- `LyricsControllerTest`: `GET /lyrics` falha → `200` com a mensagem; `GET /lyrics/cached` legado → `404`.

### Testes de Integração

- `Mp3PlayerApplicationTests` (contexto Spring) deve continuar passando após a mudança da porta.
- Validação manual com logs reais (rede): as 4 músicas do `input.txt`; checar logs `[vagalume.com.br] Tentando URL direta: .../nara-leao/insensatez.html` e `.../os-mutantes/bat-macumba.html`.

## Sequenciamento de Desenvolvimento

### Ordem de Construção

1. `toSlug` com diacríticos + testes (menor risco; resolve Nara Leão e títulos com acento).
2. `Lyric.notFoundMessage/isNotFoundMessage` + `LyricsService` (não persistir; ignorar cache legado) + testes.
3. `ArtistCandidates` + testes.
4. Evolução da porta `LyricsScraper` e `CompositeLyricsScraper` (candidatos, `Optional`) + atualização dos testes existentes e do `LyricsService`.
5. `findViaSearch` resiliente nas duas fontes.
6. `mvn test`; validação manual; atualizar `docs/sdd/atividades/ofy-012-letras/*` e `docs/sdd/TechSpec.md` (contratos de `/lyrics` e remoção da pendência "Letra não encontrada é gravada como letra").

### Dependências Técnicas

- Internet e disponibilidade do Vagalume/letras.mus.br para a validação manual. Nenhuma dependência nova (usa `java.text.Normalizer`).

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs: `INFO [Scraper] Candidatos de artista: [...]` (novo, uma linha por busca); `INFO [Scraper] Tentando fonte/candidato`; `WARN [<fonte>] Falha em <estratégia>: <mensagem>` (sem stack trace completo nas falhas esperadas de busca, para reduzir ruído no `backend.log`; hoje a falha em `gil-gilberto` imprime stack trace); `WARN [Scraper] Nenhuma fonte encontrou letra…` mantido; `INFO [Lyrics] Cache legado ignorado: <path>` (novo).

## Considerações Técnicas

### Decisões Principais

- **Candidatos de artista na porta (não só no service):** o conhecimento de "tentar várias formas do nome" fica na infraestrutura de scraping (que já itera fontes), enquanto a **origem** dos candidatos (tag/arquivo) é regra de aplicação/domínio. Alternativa rejeitada: chamar `fetch(artist,title)` várias vezes a partir do `LyricsService` — repetiria a iteração de fontes e as requisições de página de artista a cada chamada.
- **`Optional` em vez de texto "não encontrada":** elimina o acoplamento entre texto de UI e persistência (causa raiz do defeito 4). Alternativa rejeitada: manter `String` e testar o prefixo no `LyricsService` — frágil e já foi a origem do bug.
- **Cache legado tratado no `LyricsService`, não apagado:** não destrói dados do usuário (o `.txt` só é sobrescrito se a nova busca encontrar letra) e mantém `FileLyricRepository` sem regra de negócio.
- **Mensagem de falha idêntica à atual** para não mexer no `LyricsPanel` nem em `shared/api/lyrics.ts`.
- **Sem fuzzy/remoção de sufixos de título**: escopo fechado no PRD.

### Riscos Conhecidos

- **letras.mus.br não verificado fora do app:** `403` em `curl`; a correção de slug segue o padrão de URL observado no código/logs (`/gilberto-gil/`, `/nara-leao/` esperado). Mitigação: validação manual pelo app e logs.
- **Falsos positivos:** com mais candidatos, a busca por *página do artista* exige link com título exato (`toSlug(a.text())==titleSlug`) e a URL direta exige `200`, então o risco de letra errada é baixo; mesmo assim um candidato secundário pode apontar para outra música homônima (ex.: cover). Aceito; usuário pode `Alterar letra`/`Remover letra`.
- **Mais requisições / bloqueio anti-bot:** limitado por teto de 4 candidatos e pelo caminho feliz inalterado.
- **Mudança de contrato da porta** quebra `CompositeLyricsScraperTest`/`LyricsServiceTest`: atualizar junto.
- **Regex de prefixo de faixa/separadores** pode errar em nomes como `Simone e Simaria`: a tag vem primeiro e as partes são só candidatos extras (custo: algumas requisições).
- **Colisão de nome de arquivo:** `.txt` é nomeado por tag (`Gilberto Gil - Parque Industrial.txt`); comportamento existente, inalterado.

### Conformidade com Padrões

- `docs/sdd/Rules.md`: §2 (`application` depende só de portas; `domain` sem Spring/Jsoup — a nova porta usa apenas `java.util`), §3 (Template Method/Composite mantidos; log com prefixos `[Scraper]`/`[Lyrics]`; Javadoc em classes públicas), §4 (teste unitário para toda classe alterada/criada; AAA; mocks só de I/O externo), §6 (sem novas fontes; sanitização de nome de arquivo preservada).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md`.
- (A pasta `.cursor/rules` do template não existe neste repositório.)

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/lyrics/application/LyricsService.java`
- `backend/src/main/java/com/ovelha/fy/lyrics/domain/model/Lyric.java` (+ novo `ArtistCandidates.java`)
- `backend/src/main/java/com/ovelha/fy/lyrics/domain/port/LyricsScraper.java`
- `backend/src/main/java/com/ovelha/fy/lyrics/infrastructure/{AbstractLyricsSource,LetrasMusBrSource,VagalumeSource,CompositeLyricsScraper}.java`
- `backend/src/main/java/com/ovelha/fy/shared/domain/util/MusicFileNaming.java` (reuso)
- Testes: `backend/src/test/java/com/ovelha/fy/lyrics/**` (+ novos `ArtistCandidatesTest`, `VagalumeSourceTest`)
- Docs a atualizar ao final: `docs/sdd/atividades/ofy-012-letras/{PRD,TechSpec}.md`, `docs/sdd/TechSpec.md` (seção `/lyrics` e riscos de scraping)

## Registro da Implementação

- Implementado conforme a spec, sem desvios de contrato. `Lyric.isNotFoundMessage` usa `indexOf(10)` (quebra de linha) por simplicidade.
- Testes: `ArtistCandidatesTest`, `VagalumeSourceTest` (novos); `LyricTest`, `LyricsServiceTest`, `CompositeLyricsScraperTest` ampliados; `LetrasMusBrSourceTest.fallsBackToDirectUrl` ajustado (esperava slug `legião-urbana`, que era o bug). `mvn test`: 253 testes, 0 falhas.
- Pendente: validação manual com rede (as 3 músicas do `input.txt`).

## Registro do Code Review (fix-review)

- `ArtistCandidates`: prefixo numérico puro (`05 - Título.mp3`) não gera candidato; se a tag já é igual ao segmento do arquivo, não há fragmentação (`Earth, Wind & Fire`); sem tag, o segmento completo vem antes dos fragmentos. **Ajusta** a regra 4 acima (antes: "omitir segmento composto") apenas para o caso sem tag.
- `Lyric.isNotFoundMessage`: passa a exigir o formato completo `Letra não encontrada para "<título>" de <artista>` (uma linha), para não confundir notas manuais do usuário com a mensagem legada.
- `AbstractLyricsSource.toSlug`: remove hífens nas bordas.
- `LyricsService`: falhas do scraper agora são registradas em `WARN [Lyrics]`.
- Demais achados (aplicados depois): cada fonte tem orçamento de tempo próprio (`mp3.lyrics.source-budget-ms`, padrão 30000) — o estouro só interrompe os candidatos restantes daquela fonte; `CompositeLyricsScraper` delega o HTTP à fonte via `LyricsSource.fetchFromSource` (referrer/timeout/cabeçalhos da fonte; remoção de `traducao.html` em `AbstractLyricsSource`) e captura `IOException | RuntimeException` por candidato; `LyricsScraper.fetch` sem `throws IOException`; logs de página de artista sem stack trace; testes de controller.
- Porta `LyricsSource` sem Jsoup: `getUserAgent`, `findPage` e `extractLyrics` saíram da porta e viraram membros de `AbstractLyricsSource`; removida a exceção correspondente em `Rules.md` §2.
- `AbstractLyricsSource.statusCode` usa `HEAD` (com `GET` de fallback em 403/405/501) para não baixar a página duas vezes.
- `Lyric.isNotFoundMessage` também reconhece a mensagem legada sem artista (`... de `). `ArtistCandidates.TRACK_PREFIX` só remove prefixo de faixa em formatos inequívocos (`01 `, `1-05 `, `01. `), preservando `50 Cent`/`2 Unlimited`.
- Pendência técnica remanescente: o orçamento é verificado entre tentativas (não interrompe uma requisição em andamento, limitada pelos timeouts da fonte).
