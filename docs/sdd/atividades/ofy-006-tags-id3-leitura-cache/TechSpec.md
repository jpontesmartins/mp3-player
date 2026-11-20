# Especificação Técnica — ofy-006 — Leitura de tags ID3 e cache de metadados

> Levantamento retroativo (v1.6.0). Marcas `[PENDENTE / AMBÍGUO NO CÓDIGO]` indicam pontos não resolvidos pelo código.

## Resumo Executivo

`Id3Codec` é montado em `Id3CodecConfig` como `CachedId3Codec(Id3MagicCodec, MetadataCacheRepository)`. `Id3Service` expõe leitura simples, em lote (executor de virtual threads) e atualização. `FileMetadataCacheRepository` mantém `ConcurrentHashMap` e grava o JSON inteiro de forma atômica a cada `put`.

## Arquitetura do Sistema

### Visão Geral dos Componentes

- Web: `MetadataController` (`GET /id3`, `POST /id3/bulk`), `InfoController` (`GET /info`).
- Application: `Id3Service` (`getForFile`, `getBulk`, `update`, `cacheLocation`).
- Infra: `Id3MagicCodec`, `CachedId3Codec`, `FileMetadataCacheRepository`.
- Domain: `Id3Codec` (porta), `MetadataCacheRepository` (repositório), `MusicFile` + `Metadata`.
- Fluxo bulk: lista de caminhos → (se `!refresh`) separa os presentes no cache → restante: `readBulk` (1 virtual thread por arquivo) → `Id3Codec.read` → cache/disco → mapa `{caminho: tags}` (ordem preservada).

## Design de Implementação

### Interfaces Principais

```java
public interface Id3Codec { MusicFile read(String filePath); MusicFile update(String filePath, Map<String,String> tags); }
public interface MetadataCacheRepository {
    Map<String,String> get(String path); void put(String path, Map<String,String> tags);
    void putAll(Map<String,Map<String,String>> tagsByPath); String location();
}
```

### Modelos de Dados

- `MusicFile.Metadata(title, artist, album, year, genre, track, disc, Long durationMs, Integer bitrateKbps)`.
- Arquivo de cache: `{ "<caminho>": { "title": "...", "duration_ms": "215000", "_lastModified": "1788110934192" } }`.
- Local do cache: `<dir de mp3.log-file>/music-cache.json` se `mp3.log-file` estiver definido; senão `~/.mp3-player/music-cache.json`.

### Endpoints de API

| Método/caminho | Entrada | Saída |
|---|---|---|
| `GET /id3?path=` | query | mapa de tags |
| `POST /id3/bulk?refresh=false` | JSON `string[]` | `{ "<caminho>": {tags} }` |
| `GET /info` | — | `{logFile, cacheFile, backendPort, frontendPort}` |

## Pontos de Integração

- mp3agic (arquivo). Nenhuma rede.

## Abordagem de Testes

### Testes Unitários

- `Id3MagicCodecTest`, `CachedId3CodecTest`, `FileMetadataCacheRepositoryTest`, `Id3ServiceTest`, `MetadataControllerTest`, `InfoControllerTest`, `MusicFileTest`.

### Testes de Integração

- `Mp3PlayerApplicationTests` (injeção de `Id3Codec` decorado).

## Sequenciamento de Desenvolvimento

### Ordem de Construção

Leitura ID3 (1.0) → virtual threads (1.2.0) → cache de metadados (1.2.1) → cache com `lastModified` (1.3.0) → Decorator (1.3.0).

### Dependências Técnicas

- mp3agic 0.9.1; diretório gravável para o cache.

## Monitoramento e Observabilidade

- Sem métricas Prometheus/Grafana `[PENDENTE / AMBÍGUO NO CÓDIGO]`.
- Logs SLF4J do backend com o prefixo indicado nas classes envolvidas; no app instalado vão para `<app_log_dir>/backend.log` (caminho visível em Configurações → "Local do log do backend").
- Prefixos `[ID3]` (processamento, servido do cache), `[Cache]` (carga/gravação), `[Metadata]`.

## Considerações Técnicas

### Decisões Principais

- Decorator para manter o codec puro e o cache transparente.
- Cache em arquivo JSON simples (sem BD) com gravação atômica (`.tmp` + `move`).
- Virtual threads para paralelizar I/O.

### Riscos Conhecidos

- `put` regrava todo o JSON a cada arquivo lido: custo O(n²) em bibliotecas grandes na primeira leitura.
- Cache sem expurgo: entradas de arquivos removidos permanecem.
- Falha de gravação do cache é apenas logada.

### Conformidade com Padrões

- `docs/sdd/Rules.md` (§2 fronteiras, §3 padrões, §4 testes, §6 zonas sensíveis).
- `.opencode/skills/backend-arch/SKILL.md` e `.opencode/skills/backend-dev/SKILL.md` (Clean Architecture, SOLID, AAA, Javadoc).
- (A pasta `.cursor/rules` citada no template não existe neste repositório.)

### Arquivos relevantes

- `backend/src/main/java/com/ovelha/fy/player/music/{application/Id3Service.java, infrastructure/{Id3MagicCodec,CachedId3Codec,FileMetadataCacheRepository}.java, web/{MetadataController,InfoController}.java, domain/{port/Id3Codec,repository/MetadataCacheRepository}.java}`
- `backend/src/main/java/com/ovelha/fy/player/domain/model/MusicFile.java`, `backend/src/main/java/com/ovelha/fy/shared/config/Id3CodecConfig.java`
- `frontend/src/shared/api/{id3.ts,system.ts}`, `frontend/src/shared/types/index.ts`
