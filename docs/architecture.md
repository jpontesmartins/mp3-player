# Diagramas de Interação das Camadas

Diagramas [Mermaid](https://mermaid.js.org/) que mostram como uma requisição atravessa as camadas do backend (Clean Architecture/DDD) e como os objetos colaboram por caso de uso.

Todos os casos de uso seguem o mesmo padrão:

```
web (Controllers) ──> application (AppService) ──> domain ports/repositories
                                                      │
                                                      └──> infrastructure (adapters)
```

O controller **só traduz** HTTP ↔ chamadas de serviço. O service **orquestra** os ports. A infra **implementa** o acesso a arquivo/sistema/biblioteca. O domínio **não depende de nada**.

---

## 1. Visão geral das camadas e do fluxo

```mermaid
flowchart TD
    subgraph Web["web (adaptadores HTTP)"]
        PC["PlayerController"]
        PLC["PlaylistController"]
        MC["MetadataController<br/>(ID3 + cover)"]
        IC["InfoController"]
        LC["LyricsController"]
        DC["DictionaryController"]
    end

    subgraph Application["application (casos de uso)"]
        PS["PlayerService"]
        PL["PlaylistService"]
        ID["Id3Service"]
        CS["CoverService"]
        LY["LyricsService"]
        DLS["DictionaryLookupService"]
    end

    subgraph Domain["domain (núcleo — sem dependências)"]
        PORTS["ports: PlayerEngine, Id3Codec,<br/>AlbumCoverSearcher, MusicScanner,<br/>LyricsScraper, LyricsSource,<br/>DictionarySource"]
        REPOS["repositories: PlaylistRepository,<br/>LyricRepository, MetadataCacheRepository"]
        MODEL["model: Music, Playlist, Lyric,<br/>Artist, Album, CoverImage,<br/>DictionaryLookupResult, Settings"]
    end

    subgraph Infra["infrastructure (implementações)"]
        ENGINE["JLayerPlayerEngine"]
        CODEC["CachedId3Codec → Id3MagicCodec"]
        SCAN["FileMusicScanner"]
        SCRAP["CompositeLyricsScraper<br/>→ LetrasMusBrSource"]
        DICT["PriberamSource"]
        PREPO["FilePlaylistRepository"]
        LREPO["FileLyricRepository"]
        CACHE["FileMetadataCacheRepository"]
        COVER["CompositeCoverSearcher<br/>→ iTunes / Deezer"]
    end

    Request["HTTP request (frontend Tauri/React)"] --> PC
    Request --> PLC
    Request --> MC
    Request --> IC
    Request --> LC
    Request --> DC
    PC --> PS
    PLC --> PL
    MC --> ID
    MC --> CS
    LC --> LY
    DC --> DLS
    PS --> PORTS
    PL --> PORTS
    ID --> PORTS
    CS --> PORTS
    LY --> PORTS
    DLS --> PORTS
    PL --> REPOS
    LY --> REPOS
    ID --> REPOS

    ENGINE -. implementa .-> PORTS
    CODEC -. implementa .-> PORTS
    SCAN -. implementa .-> PORTS
    SCRAP -. implementa .-> PORTS
    DICT -. implementa .-> PORTS
    PREPO -. implementa .-> REPOS
    LREPO -. implementa .-> REPOS
    CACHE -. implementa .-> REPOS
    COVER -. implementa .-> PORTS
```

Alguns nós acima são ilustrativos. O essencial: **as setas que cruzam camadas apontam do detalhe para o contrato**, nunca o contrário.

---

## 2. Regra de dependência (Hexagonal / Ports & Adapters)

```mermaid
flowchart LR
    subgraph Center["domain (núcleo)"]
        direction TB
        Model["model"]
        Ports["port (interfaces)"]
    end

    subgraph App["application"]
        Svc["casos de uso (services)"]
    end

    subgraph Infra["infrastructure"]
        Impl["implementações dos ports"]
    end

    subgraph Web["web"]
        Ctrl["PlayerController / PlaylistController<br/>MetadataController / LyricsController<br/>InfoController / DictionaryController"]
    end

    Ctrl -->|depende de| Svc
    Svc -->|depende de| Ports
    Svc -->|depende de| Model
    Infra -->|"implementa (depende de)"| Ports

    style Impl fill:#333,color:#eee
    style Center fill:#2a2a2a,color:#eee
    style Application fill:#333,color:#eee
    style Web fill:#333,color:#eee
```

- **application** conhece **domain** (interfaces `port`/`repository` + `model`), nunca a `infrastructure`.
- **infra** implementa as interfaces do domínio.
- A injeção é feita no runtime pelo Spring (o detalhe é entregue ao núcleo).

---

## 3. Caso: reproduzir um MP3 (`POST /play`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend (React)
    actor Msg as HTTPS 8111
    participant C as PlayerController
    participant PS as PlayerService (application)
    participant PE as PlayerEngine (port)
    participant J as JLayerPlayerEngine (infra)

    FE->>C: POST /play (body = caminho)
    C->>PS: play(filePath)
    PS->>PS: carrega/valida a faixa atual
    PS->>J: start(path) via PE
    J-->>PS: ok (thread de áudio)
    PS-->>C: "Playing: ..."
    C-->>FE: 200 OK
```

O mesmo fluxo vale para `pause`, `stop`, `resume`, `seek`: o controller só repassa o comando e o `PlayerService` traduz para o `PlayerEngine`, que delega ao `JLayerPlayerEngine`.

---

## 4. Caso: escanear pasta física e listar playlists (`GET /playlist?path=...` e `/playlists`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant C as PlaylistController
    participant S as PlaylistService (application)
    participant Scanner as MusicScanner (port)
    participant FS as FileMusicScanner (infra)
    participant R as PlaylistRepository (port)
    participant FR as FilePlaylistRepository (infra)

    alt Escaneia pasta física
        FE->>C: GET /playlist?path=<pasta>
        C->>S: scanFolder(pasta)
        S->>FS: scanFolder(pasta)  [MusicScanner]
        FS-->>S: List<Music> (caminhos .mp3)
        S-->>C: List<String> caminhos
        C-->>FE: json: [...]
    end

    alt Lista playlists virtuais
        FE->>C: GET /playlists
        C->>S: list()
        S->>FR: list()  [PlaylistRepository]
        FR-->>S: nomes
        S-->>C: nomes
        C-->>FE: json
    end
```

- Criar (`POST /playlist`), carregar (`GET /playlist/{name}`), renomear (`/playlist/rename`) e excluir (`DELETE /playlist/{name}`) seguem o mesmo padrão, trocando apenas o método do **repositório** (`save`, `load`, `rename`, `delete`).

---

## 5. Caso: editar tags ID3 (`POST /id3/update`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant C as MetadataController
    participant S as Id3Service (application)
    participant CACHED as CachedId3Codec (decorator)
    participant M as Id3MagicCodec (infra)
    participant CACHE as MetadataCacheRepository

    FE->>C: POST /id3/update { path, tags }
    C->>S: update(path, tags)
    S->>CACHED: update(path, tags)  [Id3Codec]
    CACHED->>M: update(path, tags)
    M-->>CACHED: Music (tags atualizadas)
    CACHED->>CACHE: put(path, tags)
    CACHED-->>S: Music
    S-->>C: Map (tags resultantes)
    C-->>FE: json
```

Leitura (`GET /id3`) usa `read(path)` que verifica o cache antes de delegar. Bulk (`POST /id3/bulk`) itera com virtual threads.

---

## 6. Caso: buscar letra (`GET /lyrics`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant C as LyricsController
    participant S as LyricsService (application)
    participant R as LyricRepository (port)
    participant FR as FileLyricRepository (infra)
    participant CO as Id3Codec (port)
    participant I as Id3MagicCodec (infra)
    participant SCR as LyricsScraper (port)
    participant CLS as CompositeLyricsScraper
    participant LMB as LetrasMusBrSource
    participant WS as letras.mus.br

    FE->>C: GET /lyrics?path=<arquivo>
    C->>S: get(path)
    S->>FR: find(path)  [LyricRepository]
    alt cache hit
        FR-->>S: Optional<Lyric>
        S-->>C: texto (letra)
        C-->>FE: 200
    else cache miss
        FR-->>S: Optional.empty()
        S->>I: read(path)  [Id3Codec]
        I-->>S: Music (artista/título)
        S->>CLS: fetch(artista, título)  [LyricsScraper]
        CLS->>LMB: trySource()
        LMB->>WS: scraping letras.mus.br
        WS-->>LMB: html
        LMB-->>CLS: texto
        CLS-->>S: texto
        S->>FR: save(Lyric, Music)  [cache em .txt]
        S-->>C: texto
        C-->>FE: 200
    end
```

`GET /lyrics/cached` usa só o ramo "cache hit": `find`, e devolve `404` se não houver letra salva.

---

## 7. Caso: consultar dicionário (`POST /dictionary/lookup`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant C as DictionaryController
    participant S as DictionaryLookupService (application)
    participant SRC as DictionarySource (port)
    participant PRI as PriberamSource (infra)
    participant WEB as dicionario.priberam.org

    FE->>C: POST /dictionary/lookup { word, language }
    C->>S: lookup(word, language)
    S->>SRC: lookup(word)  [DictionarySource por língua]
    SRC->>PRI: lookup(word)
    PRI->>WEB: fetch página da palavra
    WEB-->>PRI: HTML
    PRI-->>SRC: DictionaryLookupResult
    SRC-->>S: resultado
    S-->>C: resultado
    C-->>FE: 200 OK
```

`GET /dictionary/languages` retorna as línguas suportadas (atualmente apenas `"pt"`).

---

## 8. Resumo: quem chama quem (suas por serviço infra)

```mermaid
flowchart LR
    subgraph WebC["web (adaptadores HTTP)"]
        PC["PlayerController"]
        PLC["PlaylistController"]
        MC["MetadataController"]
        IC["InfoController"]
        LC["LyricsController"]
        DC["DictionaryController"]
    end
    subgraph App["application (casos de uso)"]
        PS["PlayerService"]
        PL["PlaylistService"]
        ID["Id3Service"]
        CS["CoverService"]
        LY["LyricsService"]
        DLS["DictionaryLookupService"]
    end
    subgraph Ports["domain/port + repository (contratos)"]
        dPE["PlayerEngine"]
        dIC["Id3Codec"]
        dACS["AlbumCoverSearcher"]
        dMS["MusicScanner"]
        dLS["LyricsScraper"]
        dDS["DictionarySource"]
        dPR["PlaylistRepository"]
        dLR["LyricRepository"]
        dMCR["MetadataCacheRepository"]
    end
    subgraph Impl["infrastructure (detalhes)"]
        J["JLayerPlayerEngine"]
        IC2["CachedId3Codec → Id3MagicCodec"]
        CCS["CompositeCoverSearcher → iTunes/Deezer"]
        FS["FileMusicScanner"]
        CLS["CompositeLyricsScraper → LetrasMusBrSource"]
        PS2["PriberamSource"]
        FPR["FilePlaylistRepository"]
        FLR["FileLyricRepository"]
        FMCR["FileMetadataCacheRepository"]
    end

    PC --> PS
    PLC --> PL
    MC --> ID
    MC --> CS
    LC --> LY
    DC --> DLS

    PS --> dPE
    ID --> dIC
    CS --> dIC
    CS --> dACS
    LY --> dIC
    LY --> dLS
    PL --> dMS
    PL --> dPR
    LY --> dLR
    DLS --> dDS
    ID --> dMCR

    dPE --> J
    dIC --> IC2
    dACS --> CCS
    dMS --> FS
    dLS --> CLS
    dDS --> PS2
    dPR --> FPR
    dLR --> FLR
    dMCR --> FMCR
```

A seta `contrato --> detalhe` representa a **injeção de dependência** feita pelo Spring: o `application` depende da interface, mas recebe (em runtime) a implementação concreta.

---

## 9. Padrões de design

```mermaid
flowchart TD
    subgraph Patterns["Padrões utilizados"]
        direction TB
        P1["Ports & Adapters<br/>domain/port/ define contratos"]
        P2["Decorator<br/>CachedId3Codec envolve Id3MagicCodec"]
        P3["Template Method<br/>AbstractCoverSearcher / AbstractLyricsSource<br/>AbstractDictionarySource"]
        P4["Strategy / Composite<br/>CompositeCoverSearcher (iTunes → Deezer)<br/>CompositeLyricsScraper (letras.mus.br → ...)<br/>DictionaryLookupService (Priberam, ...)"]
        P5["Repository<br/>PlaylistRepository, LyricRepository,<br/>MetadataCacheRepository"]
        P6["Injeção de Dependência<br/>Spring injeta adapters nos services"]
    end
```

---

Para gerar estes diagramas localmente, use a extensão Mermaid do editor ou [mermaid.live](https://mermaid.live) copiando o bloco ` ```mermaid ` desejado. No GitHub eles renderizam automaticamente.
