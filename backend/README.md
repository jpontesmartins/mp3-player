# Backend — ovelhafy

API REST em **Java 21** com **Spring Boot 3.3.5**, construida sob **Clean Architecture**, **Clean Code** e **Domain-Driven Design (DDD)**.

---

## Arquitetura

O backend e organizado por **modulos de negocio** (Bounded Contexts). Cada modulo encapsula suas proprias camadas arquiteturais (`domain`, `application`, `infrastructure`, `web`).

### Principios

- **Dependencias apontam sempre para dentro**: o dominio nao conhece a infraestrutura.
- **Cada funcionalidade pertence a um unico modulo**: logica nao distribuida entre pastas.
- **Ports & Adapters**: o dominio define contratos (interfaces), a infraestrutura fornece implementacoes.
- **Injecao de dependencia via Spring**: os adapters sao injetados nos services em runtime.

### Estrutura

```
com.mp3player
├── Mp3PlayerApplication.java
│
├── shared/                                  # Compartilhado entre modulos
│   ├── domain/model/Settings.java
│   ├── domain/util/MusicFileNaming.java
│   └── config/CorsConfig.java
│
├── config/                                  # Wiring de beans entre modulos
│   ├── CoverSearcherConfig.java
│   └── Id3CodecConfig.java
│
├── player/                                  # Bounded Context: Reproducao
│   ├── domain/
│   │   ├── model/Music.java                 # Raiz agregada (aggregate root)
│   │   └── port/PlayerEngine.java
│   ├── application/
│   │   └── PlayerService.java
│   ├── infrastructure/
│   │   └── JLayerPlayerEngine.java
│   └── web/
│       └── PlayerController.java
│
├── playlist/                                # Bounded Context: Playlists
│   ├── domain/
│   │   ├── model/Playlist.java
│   │   ├── port/MusicScanner.java
│   │   └── repository/PlaylistRepository.java
│   ├── application/
│   │   └── PlaylistService.java
│   ├── infrastructure/
│   │   ├── FilePlaylistRepository.java
│   │   └── FileMusicScanner.java
│   └── web/
│       └── PlaylistController.java
│
├── metadata/                                # Bounded Context: Tags ID3 + Capas
│   ├── domain/
│   │   ├── model/{Album,Artist,CoverImage}.java
│   │   ├── port/{Id3Codec,AlbumCoverSearcher}.java
│   │   └── repository/MetadataCacheRepository.java
│   ├── application/
│   │   ├── Id3Service.java
│   │   └── CoverService.java
│   ├── infrastructure/
│   │   ├── Id3MagicCodec.java
│   │   ├── CachedId3Codec.java              # Decorator
│   │   ├── AbstractCoverSearcher.java       # Template Method
│   │   ├── CompositeCoverSearcher.java      # Strategy/Composite
│   │   ├── ItunesCoverSearcher.java
│   │   ├── DeezerCoverSearcher.java
│   │   ├── CoverDownloader.java
│   │   ├── FileMetadataCacheRepository.java
│   │   └── CoverProperties.java
│   └── web/
│       ├── MetadataController.java
│       └── InfoController.java
│
└── lyrics/                                  # Bounded Context: Letras
    ├── domain/
    │   ├── model/Lyric.java
    │   ├── port/LyricsScraper.java
    │   └── repository/LyricRepository.java
    ├── application/
    │   └── LyricsService.java
    ├── infrastructure/
    │   ├── JsoupLyricsScraper.java
    │   ├── FileLyricRepository.java
    │   └── LyricsProperties.java
    └── web/
        └── LyricsController.java
```

### Bounded Contexts e dependencias

| Modulo | Responsabilidade | Depende de |
|---|---|---|
| **shared** | Config CORS, Settings, MusicFileNaming | — |
| **player** | Reproducao de audio (raiz agregada: `Music`) | — |
| **playlist** | CRUD de playlists + scan de pastas | `player` (usa `Music`) |
| **metadata** | Tags ID3, capas, cache de metadados | `player` (usa `Music`) |
| **lyrics** | Busca de letras (web scraping + cache) | `metadata` (usa `Id3Codec`), `player` (usa `Music`) |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 (virtual threads) |
| Framework | Spring Boot 3.3.5 |
| Build | Maven |
| Testes | JUnit 5, Mockito, spring-boot-starter-test |
| Decodificador MP3 | JLayer 1.0.1 |
| Tags ID3 | mp3agic 0.9.1 |
| Web scraping | Jsoup 1.18.1 |
| Logging | SLF4J + Logback |

---

## Endpoints

### Player (`player/web/PlayerController`)

| Metodo | Rota | Descricao |
|---|---|---|
| `POST` | `/play` | Inicia reproducao (body = caminho do arquivo) |
| `POST` | `/pause` | Pausa a musica atual |
| `POST` | `/resume` | Retoma a musica pausada |
| `POST` | `/stop` | Para a reproducao e limpa o estado |
| `POST` | `/seek` | Salta para posicao (`{ "position": <ms> }`) |
| `GET` | `/playing` | Status atual (playing/paused/stopped), posicao, duracao e ID3 |

### Playlist (`playlist/web/PlaylistController`)

| Metodo | Rota | Descricao |
|---|---|---|
| `GET` | `/playlist?path=<pasta>` | Escaneia pasta e retorna arquivos `.mp3` |
| `GET` | `/playlists` | Lista playlists virtuais existentes |
| `GET` | `/playlist/{name}` | Carrega uma playlist virtual |
| `POST` | `/playlist` | Cria/atualiza playlist virtual (`{ "name", "paths" }`) |
| `DELETE` | `/playlist/{name}` | Exclui playlist virtual |
| `POST` | `/playlist/rename` | Renomeia playlist (`{ "oldName", "newName" }`) |

### Metadata (`metadata/web/MetadataController`)

| Metodo | Rota | Descricao |
|---|---|---|
| `GET` | `/id3?path=<arquivo>` | Tags ID3 de um arquivo |
| `POST` | `/id3/bulk` | Tags ID3 de varios arquivos (body = lista de caminhos) |
| `POST` | `/id3/update` | Atualiza tags (`{ "path", "tags" }`) |
| `GET` | `/cover?path=<arquivo>` | Capa do album (jpg/png/webp/gif) |
| `POST` | `/cover/download` | Baixa capa via iTunes/Deezer (`{ "path" }`) |

### Lyrics (`lyrics/web/LyricsController`)

| Metodo | Rota | Descricao |
|---|---|---|
| `GET` | `/lyrics?path=<arquivo>` | Letra (web scraping se nao houver cache) |
| `GET` | `/lyrics/cached?path=<arquivo>` | Letra apenas do cache |
| `POST` | `/lyrics` | Salva/edita letra (`{ "path", "text" }`) |
| `DELETE` | `/lyrics?path=<arquivo>` | Remove letra do cache |

### Info (`metadata/web/InfoController`)

| Metodo | Rota | Descricao |
|---|---|---|
| `GET` | `/info` | Log, cache, portas do sistema |

---

## Padroes de design

| Padrao | Onde | O que faz |
|---|---|---|
| Ports & Adapters | `domain/port/` | Dominio define contratos; infraestrutura implementa |
| Decorator | `CachedId3Codec` | Envolve `Id3MagicCodec` com cache transparente |
| Template Method | `AbstractCoverSearcher` | Define fluxo de busca; subclasses implementam passos |
| Strategy / Composite | `CompositeCoverSearcher` | Encadeia iTunes -> Deezer (fallback) |
| Repository | `PlaylistRepository`, etc. | Abstrai persistencia (hoje TXT, facilmente trocavel) |
| Injecao de dependencia | Spring | Injeta adapters nos services via construtor |

---

## Persistencia

| Dado | Localizacao | Implementacao |
|---|---|---|
| Playlists virtuais | `~/.mp3-player/playlists/<nome>.txt` | `FilePlaylistRepository` |
| Letras | `<pasta do mp3>/<artista> - <musica>.txt` | `FileLyricRepository` |
| Capas baixadas | `<pasta do mp3>/cover.<ext>` | `CoverService` |
| Cache de metadados | `~/.mp3-player/metadata-cache.json` | `FileMetadataCacheRepository` |

---

## Cache de metadados ID3

Ler tags ID3 de cada arquivo MP3 e uma operacao custosa. Para evitar reler os mesmos arquivos toda vez, a aplicacao mantem um **cache em disco** (`~/.mp3-player/metadata-cache.json`).

```
1. CachedId3Codec.read(caminho)
2. -> MetadataCacheRepository.get(caminho)
3.    Hit:  retorna Music do cache (sem ler arquivo)
4.    Miss: delega para Id3MagicCodec.read(caminho)
5.           -> armazena resultado no cache via MetadataCacheRepository.put()
6.           -> retorna Music
```

---

## Como rodar

### Pre-requisitos

- **Java 21**
- **Maven 3.8+**

### Executar

```bash
cd backend
mvn spring-boot:run
```

A API roda em `http://localhost:8111`.

### Testes

```bash
cd backend
mvn test
```

### Empacotar

```bash
mvn package -DskipTests
```

Gera o fat JAR em `target/mp3-player-<versao>.jar`.