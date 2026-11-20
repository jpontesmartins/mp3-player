# Referências — ofy-018 — Nova fonte de letras (LRCLIB)

## Origem da demanda

Teste manual do usuário após a `ofy-017` (2026-11-20): *Nara Leão – O Que Será?* e *Nara Leão – Ela Desatinou* continuaram "não encontradas". Análise registrada abaixo.

Faixas (pasta `Nara Leão (1980) - Com Açúcar Com Afeto`), tags do `music-cache.json`:
- `03 - Ela Desatinou.mp3` — `artist=Nara Leão`, `title=Ela Desatinou`, `album=Com Açúcar Com Afeto`, `duration_ms=169354`; `albumArtist=Nara Leão`; sem `composer`/`originalArtist`.
- `05 - O Que Será (À Flor Da Pele).mp3` — `artist=Nara Leão`, `title=O Que Será? (À Flor Da Pele)`, `duration_ms=241668`; sem `composer`/`originalArtist`.

## Verificações realizadas (planejamento, 2026-11-20)

**Vagalume (slug ASCII correto, música ausente sob a intérprete):**

| URL | Status |
|---|---|
| `vagalume.com.br/nara-leao/ela-desatinou.html` | 404 |
| `vagalume.com.br/nara-leao/o-que-sera-a-flor-da-pele.html` | 404 |
| `vagalume.com.br/chico-buarque/ela-desatinou.html` | **200** |
| `vagalume.com.br/chico-buarque/o-que-sera-a-flor-da-pele.html` | **200** |

Conclusão: o site cataloga pelo compositor/intérprete original; covers não aparecem na página da intérprete.

**Buscas internas dos sites:**
- Vagalume `/search?q=` → 200, mas é um widget Google CSE (`<gcse:searchresults-only>`) renderizado por JavaScript; `search.php?q=` → 404; `api.vagalume.com.br/search.*` → 503.
- letras.mus.br → 403 inclusive com Jsoup nos testes de planejamento (anti-bot); nas execuções do app de 2026-11-20 11:01–11:06 as páginas de artista carregaram (logs). Estado instável/dependente de cabeçalhos.

**LRCLIB (`https://lrclib.net`):**
- `GET /api/get?track_name=Ela+Desatinou&artist_name=Nara+Le%C3%A3o&duration=169` → 200, `trackName="Ela Desatinou"`, `artistName="Nara Leão"`, `albumName="Com Açúcar Com Afeto"`, `duration=169.466667`, `instrumental=false`, `plainLyrics` preenchido, `syncedLyrics` presente.
- `GET /api/search?track_name=Ela+Desatinou&artist_name=Nara+Le%C3%A3o` → 4 resultados de Nara Leão (um como `Leão, Nara`).
- `GET /api/search?track_name=Ela+Desatinou` → 20 resultados (Chico Buarque etc.).
- `GET /api/search?track_name=O+Que+Ser%C3%A1%3F+(%C3%80+Flor+Da+Pele)&artist_name=Nara+Le%C3%A3o` → **0**; `…track_name=O+Que+Ser%C3%A1+(%C3%80+Flor+Da+Pele)` (sem `?`, sem artista) → 20 (Chico Buarque, Milton Nascimento feat. Chico, Simone…); `…track_name=O+Que+Ser%C3%A1&artist_name=Nara+Le%C3%A3o` → 0.
- Cabeçalhos: `access-control-allow-origin: *`, atrás de Cloudflare; resposta `Content-Type: application/json`.
- Campos da resposta: `id`, `name`, `trackName`, `artistName`, `albumName`, `duration`, `instrumental`, `plainLyrics`, `syncedLyrics`, `lyricsfile`, `hasWordSync`.

Implicação: *O Que Será* por Nara Leão provavelmente **não existe** no LRCLIB como gravação dela; só será atendida pela regra RF-018.3(d) (mesma música, outro intérprete). Decisão a validar (ver PRD, Questões em Aberto).

## Código analisado

- `backend/.../lyrics/domain/port/LyricsSource.java` — contrato por página/Jsoup (`findPage`, `extractLyrics(Document)`), motivo da nova porta.
- `backend/.../lyrics/infrastructure/CompositeLyricsScraper.java` (pós-ofy-017) — itera `LyricsSource × candidatos`; ponto de extensão para fontes de API.
- `backend/.../lyrics/infrastructure/{LyricsConfig,LyricsProperties}.java`, `resources/application.properties` — propriedades `mp3.lyrics.{letras,vagalume}.*` (prioridades: Vagalume 1, letras 2; nova fonte = 3).
- `backend/.../lyrics/application/LyricsService.java` — onde `LyricsQuery` será montado.
- `backend/.../player/domain/model/MusicFile.java` — `Metadata(title, artist, album, year, genre, track, disc, durationMs, bitrateKbps)` fornece álbum e duração.
- `backend/.../lyrics/domain/model/ArtistCandidates.java` (ofy-017) — candidatos de artista reutilizados.
- Specs relacionadas: `docs/sdd/atividades/ofy-012-letras/*`, `docs/sdd/atividades/ofy-017/*`, `docs/sdd/Rules.md` §2/§6.

## Payload de exemplo (resposta `/api/get`, truncada)

```json
{
  "id": 17612830,
  "trackName": "Ela Desatinou",
  "artistName": "Nara Leão",
  "albumName": "Com Açúcar Com Afeto",
  "duration": 169.466667,
  "instrumental": false,
  "plainLyrics": "Ela desatinou, viu chegar quarta-feira\nAcabar brincadeira, ...",
  "syncedLyrics": "[00:03.73] Ela desatinou, viu chegar quarta-feira\n..."
}
```

Requisição equivalente do app: `GET https://lrclib.net/api/get?track_name=Ela%20Desatinou&artist_name=Nara%20Le%C3%A3o&album_name=Com%20A%C3%A7%C3%BAcar%20Com%20Afeto&duration=169` com `User-Agent: ovelhafy-mp3-player (lrclib client)`.

## Decisões técnicas do planejamento

1. Nova porta `LyricsApiSource` (sem Jsoup) em vez de generalizar `LyricsSource`.
2. `LyricsScraper.fetch(LyricsQuery)`; `LyricsQuery(artistCandidates, title, album, durationMs)`.
3. Ordem: páginas (Vagalume, letras.mus.br) → API (LRCLIB, prioridade 3).
4. `HttpClient` JDK + Jackson; sem dependências novas.
5. Somente `plainLyrics`; `instrumental=true` descartado.
6. Teto de 6 requisições por música; sem retry; sem paralelismo.
7. Variações de título apenas para a nova fonte.
8. Escolha de resultado: (a) artista+duração ±5 s, (b) artista, (c) título idêntico+duração, (d) título idêntico.

## Premissas a validar antes do `/dev`

- Uso do LRCLIB aceito (termos/privacidade: só título, artista, álbum, duração; `User-Agent` genérico).
- Regra (d) aceita (letra de outro intérprete quando não há a da intérprete), ou exigir artista igual.
- Teto de 6 requisições e eventual teto global de tempo.
- Dependência da `ofy-017` já concluída e commitada (atualmente com alterações não commitadas na árvore de trabalho).
