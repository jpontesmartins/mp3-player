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
        LC["LyricsController"]
    end

    subgraph Application["application (casos de uso)"]
        PS["PlayerService"]
        PL["PlaylistAppService"]
        LY["LyricsAppService"]
        ID["Id3AppService"]
    end

    subgraph Domain["domain (núcleo — sem dependências)"]
        PORTS["ports: PlayerEngine, Id3Codec,<br/>MusicScanner, LyricsScraper"]
        REPOS["repositories: PlaylistRepository,<br/>LyricRepository"]
        MODEL["model: Music, Playlist, Lyric,<br/>Artist, Album, Settings"]
    end

    subgraph Infra["infrastructure (implementações)"]
        ENGINE["JLayerPlayerEngine"]
        CODEC["Id3MagicCodec (mp3agic)"]
        SCAN["FileMusicScanner"]
        SCRAP["JsoupLyricsScraper"]
        PREPO["FilePlaylistRepository"]
        LREPO["FileLyricRepository"]
    end

    Request["HTTP request (frontend Tauri/React)"] --> PC
    Request --> PLC
    Request --> MC
    Request --> LC
    PC --> PS
    PLC --> PL
    MC --> ID
    LC --> LY
    PS --> PORTS
    PL --> PORTS
    LY --> PORTS
    ID --> PORTS
    PL --> MODEL
    ID --> MODEL
    LY --> MODEL
    PL --> REPOS
    LY --> REPOS

    ENGINE -. implementa .-> PORTS
    CODEC -. implementa .-> PORTS
    SCAN -. implementa .-> PORTS
    SCRAP -. implementa .-> PORTS
    PREPO -. implementa .-> REPOS
    LREPO -. implementa .-> REPOS
```

Alguns nós acima são ilustrativos (PL/PS etc. correspondem ao desenho da árvore de pacotes). O essencial: **as setas que cruzam camadas apontam do detalhe para o contrato**, nunca o contrário.

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
        Ctrl["PlayerController / PlaylistController<br/>MetadataController / LyricsController"]
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
    actor Msg as HTTPS 8080
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
    participant S as PlaylistAppService (application)
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
    participant S as Id3AppService (application)
    participant CO as Id3Codec (port)
    participant M as Id3MagicCodec (infra)

    FE->>C: POST /id3/update { path, tags }
    C->>S: update(path, tags)
    S->>M: update(path, tags)  [Id3Codec]
    M-->>S: Music (tags atualizadas)
    S-->>C: Map (tags resultantes)
    C-->>FE: json
```

Leitura (`GET /id3`) e bulk (`POST /id3/bulk`) usam `read(path)` / iteração do mesmo `Id3Codec`.

---

## 6. Caso: buscar letra (`GET /lyrics`)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant C as LyricsController
    participant S as LyricsAppService (application)
    participant R as LyricRepository (port)
    participant FR as FileLyricRepository (infra)
    participant CO as Id3Codec (port)
    participant I as Id3MagicCodec (infra)
    participant SCR as LyricsScraper (port)
    participant J as JsoupLyricsScraper (infra)
    participant WS as letras.mus.br

    FE->>C: GET /lyrics?path=<arquivo>
    C->>S: get(path)
    S->>FR: exists(path)  [LyricRepository]
    alt cache hit
        FR-->>S: true
        S->>FR: find(path)
        FR-->>S: Lyric
        S-->>C: texto (letra)
        C-->>FE: 200
    else cache miss
        FR-->>S: false
        S->>I: read(path)  [Id3Codec]
        I-->>S: Music (artista/título)
        S->>J: fetch(artista, título)  [LyricsScraper]
        J->>WS: scraping letras.mus.br
        WS-->>J: html
        J-->>S: texto
        S->>FR: save(Lyric, Music)  [cache em .txt]
        S-->>C: texto
        C-->>FE: 200
    end
```

`GET /lyrics/cached` usa só o ramo "cache hit": `exists` → `find`, e devolve `404` se não houver letra salva.

---

## 7. Resumo: quem chama quem (suas por serviço infra)

```mermaid
flowchart LR
    subgraph WebC["web (adaptadores HTTP)"]
        PC["PlayerController"]
        PLC["PlaylistController"]
        MC["MetadataController"]
        LC["LyricsController"]
    end
    subgraph App["application (casos de uso)"]
        PS["PlayerService"]
        PL["PlaylistService"]
        LY["LyricsService"]
        ID["Id3Service"]
    end
    subgraph Ports["domain/port + repository (contratos)"]
        dPE["PlayerEngine"]
        dIC["Id3Codec"]
        dMS["MusicScanner"]
        dLS["LyricsScraper"]
        dPR["PlaylistRepository"]
        dLR["LyricRepository"]
    end
    subgraph Impl["infrastructure (detalhes)"]
        J["JLayerPlayerEngine"]
        IC2["Id3MagicCodec"]
        FS["FileMusicScanner"]
        JS["JsoupLyricsScraper"]
        FPR["FilePlaylistRepository"]
        FLR["FileLyricRepository"]
    end

    PC --> PS
    PLC --> PL
    MC --> ID
    LC --> LY

    PS --> dPE
    ID --> dIC
    LY --> dIC
    PL --> dMS
    LY --> dLS
    PL --> dPR
    LY --> dLR

    dPE --> J
    dIC --> IC2
    dMS --> FS
    dLS --> JS
    dPR --> FPR
    dLR --> FLR
```

A seta `contrato --> detalhe` representa a **injeção de dependência** feita pelo Spring: o `application` depende da interface, mas recebe (em runtime) a implementação concreta.

---

Para gerar estes diagramas localmente, use a extensão Mermaid do editor ou [mermaid.live](https://mermaid.live) copiando o bloco ` ```mermaid ` desejado. No GitHub eles renderizam automaticamente.