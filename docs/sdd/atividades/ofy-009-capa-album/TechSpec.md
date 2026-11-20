# Especificação Técnica — ofy-009 — Capa do álbum (exibição e download)

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

`CoverArt.tsx` consome `GET /cover` e `POST /cover/download`. No backend, `MetadataController` serve a imagem existente; `CoverService` monta a consulta com as tags (`Id3Codec`) e delega a `AlbumCoverSearcher` (`CompositeCoverSearcher[iTunes, Deezer]`, montado em `CoverSearcherConfig`), que usa Template Method (`AbstractCoverSearcher`) e `CoverDownloader` (Jsoup).

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Frontend: `features/playback/ui/CoverArt.tsx`, `shared/api/cover.ts`, `shared/ui/ContextMenu.tsx`, `PlayerContext.showCover`.
- Backend: `MetadataController#getCover/#downloadCover`, `CoverService`, `CompositeCoverSearcher`, `ItunesCoverSearcher`, `DeezerCoverSearcher`, `AbstractCoverSearcher`, `CoverDownloader`, `CoverProperties`, `CoverSearcherConfig`.
- Fluxo download: `CoverService.download(path)` → `Id3Codec.read` → consulta → `findCover` (JSON: iTunes `artworkUrl100`→`600x600bb`; Deezer `cover_xl`, senão `cover_medium`) → `CoverDownloader.download(url)` → `Files.write(<pasta>/cover.<ext>)` → devolve o caminho salvo.

## Design de Implementação

### Interfaces Principais

```java
public interface AlbumCoverSearcher { CoverImage findCover(String query) throws IOException; }
public record CoverImage(byte[] bytes, String contentType) { boolean isEmpty(); }
```
```ts
getCoverUrl(path: string): string
downloadCover(path: string): Promise<{ ok: boolean; text: string }>
```

### Modelos de Dados

- `CoverProperties(userAgent, itunesUrl, deezerUrl, timeoutConnect, timeoutDownload)` com prefixo `mp3.cover`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /cover?path=<mp3>` | query | `200` imagem (`image/jpeg`, `image/png`, `image/webp` ou `image/gif`) / `404` |
| `POST /cover/download` | `{ "path": "<mp3>" }` | `200` caminho do arquivo salvo / `400 "Caminho é obrigatório"` / `400 "Erro ao baixar capa: ..."` |

## Pontos de Integração

- iTunes Search API (`mp3.cover.itunes-url`, `limit=1`) e Deezer (`mp3.cover.deezer-url`, `limit=1`), via Jsoup com `User-Agent` `Mozilla/5.0 (compatible; MP3Player/1.0)`. Sem autenticação; falhas são propagadas como `IOException` e tratadas em `CoverService`/controller.

## Abordagem de Testes

### Testes Unitários

- `CoverServiceTest`, `CompositeCoverSearcherTest`, `DeezerCoverSearcherTest`, `ItunesCoverSearcherTest`, `MetadataControllerTest`, `CoverImageTest`; frontend: `CoverArt.test.tsx`.

### Testes de Integração

- Não há (APIs externas não são exercitadas).

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Exibição de capa (1.0) → download iTunes/Deezer (1.0) → Strategy (1.3.0) e Template Method (1.3.0) → correção Deezer (1.4.0) → propriedades externalizadas (1.2.3).

### Dependências Técnicas

- Internet para download; permissão de escrita na pasta do MP3.

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixo `[Capa]` (consulta, download, fonte sem resultado).

## Considerações Técnicas

### Decisões Principais

- Composite com fallback (iTunes → Deezer); URLs e timeouts externalizados.
- Extração por regex sobre o JSON (sem biblioteca JSON), mais simples e frágil a mudanças de formato.

### Riscos Conhecidos

- Resultado incorreto (primeiro hit da API, `limit=1`).
- Mudança de formato das APIs quebra as regex.
- Sobrescrita de `cover.<ext>` existente sem confirmação.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)
- `.opencode/skills/frontend-style/SKILL.md` (frontend).

### Arquivos relevantes

- `frontend/src/features/playback/ui/CoverArt.tsx`, `frontend/src/shared/api/cover.ts`
- `backend/src/main/java/com/ovelha/fy/player/music/{application/CoverService.java, web/MetadataController.java, infrastructure/{AbstractCoverSearcher,CompositeCoverSearcher,ItunesCoverSearcher,DeezerCoverSearcher,CoverDownloader,CoverProperties}.java, domain/{model/CoverImage,port/AlbumCoverSearcher}.java}`
- `backend/src/main/java/com/ovelha/fy/shared/config/CoverSearcherConfig.java`, `backend/src/main/resources/application.properties`
