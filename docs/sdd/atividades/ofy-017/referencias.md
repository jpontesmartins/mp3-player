# Referências — ofy-017 — Bugfix: falha no download de letras

## Demanda original (`input.txt`)

Falham: Gilberto Gil – Parque Industrial; Gilberto Gil – Bat Macumba; Nara Leão – Insensatez. Funciona: Iggy Pop – The Passenger. Pedido: analisar código e logs.

## Logs analisados

Fonte: `C:\Users\joao_\AppData\Local\com.mp3player.app\logs\backend.log` (execução de 2026-11-20).

**Funciona — Iggy Pop (tag `artist=Iggy Pop`, `title=The Passenger`):**
```
[vagalume.com.br] Tentando URL direta: https://www.vagalume.com.br/iggy-pop/the-passenger.html   (200)
[vagalume.com.br] Buscando página de letra: https://www.vagalume.com.br/iggy-pop/the-passenger.html
[Lyrics] Letra salva em ...\Iggy Pop - The Passenger.txt
```

**Falha — Parque Industrial (tag `artist=Gilberto Gil`; arquivo `1-05 Gilberto Gil, Caetano Veloso, Gal Costa e Os Mutantes - Parque Industrial.mp3`):**
```
[vagalume.com.br] Tentando URL direta: .../gilberto-gil/parque-industrial.html        (não 200)
[vagalume.com.br] Tentando URL direta: .../gil-gilberto/parque-industrial.html
[vagalume.com.br] Nenhum link exato para "parque-industrial" na página .../gilberto-gil/
[vagalume.com.br] Falha ao buscar página do artista .../gil-gilberto/               (+ stack trace)
[vagalume.com.br] Buscando: .../search.php?q=Gilberto+Gil+Parque+Industrial
[Scraper] [vagalume.com.br] Falha ao buscar letra: HTTP error fetching URL. Status=404, URL=[.../search.php?q=...]
[letras.mus.br] Nenhum link exato para "parque-industrial" na página .../gilberto-gil/
[letras.mus.br] Tentando URL direta: .../gilberto-gil/parque-industrial/ , .../gil-gilberto/parque-industrial/
[letras.mus.br] Buscando: https://www.letras.mus.br/?q=Gilberto+Gil+Parque+Industrial
[letras.mus.br] Nenhuma URL encontrada
[Scraper] Nenhuma fonte encontrou letra para "Gilberto Gil" - "Parque Industrial"
[Lyrics] Letra salva em ...\Gilberto Gil - Parque Industrial.txt        <-- grava a mensagem de erro
```
Mesmo padrão para *Bat Macumba* (arquivo `1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3`) e para *Miserere Nóbis* (outra faixa afetada, não citada na demanda: slug `miserere-nóbis`).

**Insensatez (Nara Leão):** a execução não aparece no log atual (rotacionado/anterior), mas o resultado persistido confirma a falha: `Nara Leão - Dez anos depois\Nara Leão - Insensatez.txt` contém `Letra não encontrada para "Insensatez" de Nara Leão`. Causa consistente com o defeito 1 (slug `nara-leão`).

Conteúdo dos `.txt` gravados (evidência do defeito 4): `Gilberto Gil - Bat Macumba.txt`, `… Parque Industrial.txt`, `… Miserere Nóbis.txt` e `Nara Leão - Insensatez.txt` contêm somente a mensagem `Letra não encontrada para "…" de …`.

## Verificações HTTP realizadas na análise (2026-11-20, `curl` com o mesmo User-Agent do app)

| URL | Status | Conclusão |
|---|---|---|
| `vagalume.com.br/gilberto-gil/parque-industrial.html` | 404 | Música não está catalogada sob Gilberto Gil |
| `vagalume.com.br/os-mutantes/parque-industrial.html` | **200** | Existe sob Os Mutantes |
| `vagalume.com.br/gilberto-gil/bat-macumba.html` | 404 | idem |
| `vagalume.com.br/os-mutantes/bat-macumba.html` | **200** | idem |
| `vagalume.com.br/nara-leao/insensatez.html` | **200** | Slug ASCII funciona; contém `id="lyrics"` (seletor `div#lyrics` válido) |
| `vagalume.com.br/nara-leão/insensatez.html` | 404 | Slug com acento (o que o código gera hoje) falha |
| `vagalume.com.br/gilberto-gil/miserere-nóbis.html` / `…-nobis.html` | 404 / 404 | Título não localizado nessa forma (investigar na implementação: pode estar sob outro artista/título) |
| `vagalume.com.br/search.php?q=…` | 404 | Busca interna inoperante |
| `api.vagalume.com.br/search.php?art=…&mus=…` | 503 | API não utilizável |
| `letras.mus.br/*` (curl) | 403 | Anti-bot para clientes sem comportamento de navegador; o Jsoup do app obteve 200 nas páginas de artista (logs) — validar em runtime |

## Código analisado

- `backend/.../lyrics/application/LyricsService.java` — `get`: cache → tags → `fetchOrFallback` → **sempre** `lyricRepository.save` (L51–62); `artistOrFilename`/`titleOrFilename` usam o nome do arquivo apenas quando a tag está vazia (L115–131).
- `backend/.../lyrics/infrastructure/AbstractLyricsSource.java` — `toSlug` (L160–165): `replaceAll("[^a-z0-9áéíóúãõâêîôûçñ\\s]", "")` **mantém acentos**; `artistSlugs` gera direto/sem "The"/invertido (o invertido `gil-gilberto` só gera ruído: 1 requisição + stack trace por fonte).
- `…/VagalumeSource.java` — `findPage`: URL direta → página do artista → busca; `findViaSearch` sem `try/catch` (L165–175).
- `…/LetrasMusBrSource.java` — idem (L167–177); `extractSearchResult` aceita qualquer link `letras.mus.br` sem `?` (risco de falso positivo, não tratado aqui).
- `…/CompositeLyricsScraper.java` — `fetch` devolve texto `Letra não encontrada…` (L56–57); `trySource` captura `IOException` e usa referrer/timeout fixos (L82–96, pendência já registrada em `ofy-012`).
- `…/web/LyricsController.java` — contratos de `GET /lyrics`, `GET /lyrics/cached`.
- `…/infrastructure/FileLyricRepository.java` — `find` lê o `.txt` sem inspecionar o conteúdo.
- `shared/domain/util/MusicFileNaming.java` — `artistFromFilename` retorna tudo antes do primeiro ` - ` (inclui prefixo de faixa, ex.: `1-05 Gilberto Gil, ...`).
- Testes existentes a ajustar: `LyricsServiceTest`, `CompositeLyricsScraperTest`, `LetrasMusBrSourceTest`, `LyricsControllerTest`, `FileLyricRepositoryTest` (apenas se `exists` mudar).
- Specs relacionadas: `docs/sdd/atividades/ofy-012-letras/{PRD,TechSpec,referencias}.md`, `docs/sdd/TechSpec.md` (seções `/lyrics` e riscos de scraping).

## Causas-raiz (resumo rastreável)

| # | Causa | Evidência | Músicas afetadas |
|---|---|---|---|
| 1 | `toSlug` mantém diacríticos | `nara-leão` → 404, `nara-leao` → 200 | Insensatez, Miserere Nóbis, Geléia Geral… |
| 2 | Artista único (tag) vs artista no site | `os-mutantes/*` 200; `gilberto-gil/*` 404; nome do arquivo cita "Os Mutantes" | Parque Industrial, Bat Macumba |
| 3 | Buscas dos sites inoperantes + sem tolerância | `search.php` 404; `/?q=` sem resultado | todas as que dependem do plano B |
| 4 | Falha persistida como letra | `.txt` com `Letra não encontrada…` | todas as falhas (bloqueia nova tentativa) |

## Payloads de exemplo (contrato mantido)

`GET /lyrics?path=C:\...\1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3`
- Hoje (falha): `200` `Letra não encontrada para "Bat Macumba" de Gilberto Gil` **e** grava `Gilberto Gil - Bat Macumba.txt`.
- Esperado: `200` com a letra (via candidato `Os Mutantes`) e grava `Gilberto Gil - Bat Macumba.txt` com a letra; em falha total: `200` com a mensagem e **nenhum** arquivo.

## Decisões técnicas do planejamento

1. Slug ASCII via `Normalizer` NFD (sem biblioteca nova).
2. Candidatos de artista: tag primeiro, depois partes do nome do arquivo, máx. 4.
3. Porta `LyricsScraper` evolui para `Optional<String> fetch(List<String>, String)`; mensagem amigável montada no `LyricsService`.
4. Cache legado "não encontrada" ignorado, não apagado.
5. Estratégias de busca internas tornadas tolerantes a falha; não serão consertadas (endpoint de terceiros).
6. Inversão de nome de artista (`gil-gilberto`) mantida por ora (fora de escopo); candidata a remoção futura por só gerar tráfego.

## Premissas a validar antes do `/dev`

- Aceitar que `Parque Industrial`/`Bat Macumba` sejam encontradas pelo artista inferido do arquivo (e não pela tag).
- Aceitar a mudança de assinatura da porta `LyricsScraper`.
- Confirmar o tratamento do `.txt` legado (ignorar vs. apagar).
- *Miserere Nóbis* (Gilberto Gil) não foi localizada com slug ASCII na verificação; pode permanecer "não encontrada" — não faz parte dos critérios de aceite.
