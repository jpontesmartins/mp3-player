# Backend — ovelhafy API

API REST em **Java 21** com **Spring Boot 3.3.5** para reprodução de MP3 e serviços auxiliares.

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

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Reprodução MP3 | Play, pausa, stop, resume e seek via JLayer |
| Navegação entre faixas | Próxima/anterior com modos contínuo, aleatório e repetição |
| Tags ID3 | Leitura, escrita e edição em lote via mp3agic |
| Capa do álbum | Busca automática via APIs iTunes (fallback: Deezer), download e serviço local |
| Letras (lyrics) | Web scraping em letras.mus.br com múltiplos fallbacks e cache local |
| Playlists físicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | CRUD completo (criar, editar, renomear, excluir, listar) |
| Cache de metadados | Cache em disco via Decorator Pattern (`CachedId3Codec`) |
| Info do sistema | Endpoint de informações (log, cache, portas) |

## Arquitetura

O código segue **Clean Architecture** / **Clean Code** / **DDD**. A regra de dependência aponta sempre para dentro: o **domínio** é o núcleo, o **application** orquestra os casos de uso, a **infrastructure** implementa os ports e o **controller** expõe os HTTP endpoints.

```
com.mp3player
├── domain/                 # regras de negócio — sem dependências externas
│   ├── model/              #   entidades: Music, Artist, Album, Playlist, Lyric, Settings, CoverImage
│   ├── port/               #   contratos: PlayerEngine, Id3Codec, MusicScanner, LyricsScraper, AlbumCoverSearcher
│   ├── repository/         #   portas de persistência: PlaylistRepository, LyricRepository, MetadataCacheRepository
│   └── util/               #   MusicFileNaming (utilitário DRY)
├── application/            # casos de uso — orquestram ports e modelos
│   ├── player/             #   PlayerService       (play, pause, stop, resume, seek, próxima/anterior)
│   ├── playlist/           #   PlaylistService     (playlists virtuais + físico)
│   ├── lyrics/             #   LyricsService       (busca, web scraping, cache)
│   └── metadata/           #   Id3Service (ler, bulk e atualizar tags), CoverService (baixar capa)
├── infrastructure/         # implementações concretas dos ports
│   ├── audio/              #   JLayerPlayerEngine
│   ├── metadata/           #   Id3MagicCodec (mp3agic), CachedId3Codec (Decorator)
│   ├── music/              #   FileMusicScanner
│   ├── lyrics/             #   JsoupLyricsScraper
│   ├── cover/              #   AbstractCoverSearcher, ItunesCoverSearcher, DeezerCoverSearcher,
│   │                       #   CompositeCoverSearcher, CoverDownloader
│   └── repository/         #   FilePlaylistRepository, FileLyricRepository, FileMetadataCacheRepository
├── controller/             # adaptadores HTTP
│   ├── PlayerController    # /play, /pause, /stop, /resume, /seek, /playing
│   ├── PlaylistController  # /playlist, /playlists, /playlist/{name}, /playlist/rename
│   ├── MetadataController  # /id3, /id3/bulk, /id3/update, /cover, /cover/download
│   ├── LyricsController    # /lyrics, /lyrics/cached
│   └── InfoController      # /info
└── config/                 # CORS, beans, propriedades
```

### Padrões de design aplicados

| Padrão | Onde | O que faz |
|---|---|---|
| Ports & Adapters (Hexagonal) | Domain define interfaces | Infrastructure fornece implementações |
| Decorator | `CachedId3Codec` | Envolv `Id3MagicCodec` com cache transparente |
| Template Method | `AbstractCoverSearcher` | Define fluxo de busca; subclasses implementam passos específicos |
| Strategy / Composite | `CompositeCoverSearcher` | Encadeia múltiplos buscadors (iTunes → Deezer fallback) |
| Repository | `PlaylistRepository`, etc. | Abstrai persistência, facilmente trocável |
| Injeção de dependência | Spring | Injeta adapters nos services via construtor |

## Endpoints

### Reprodução

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/play` | Inicia reprodução de um arquivo MP3 (body = caminho completo) |
| `POST` | `/pause` | Pausa a música atual |
| `POST` | `/resume` | Retoma a música pausada |
| `POST` | `/stop` | Para a reprodução e limpa o estado |
| `POST` | `/seek` | Salta para uma posição específica (`{ "position": <ms> }`) |
| `GET` | `/playing` | Status atual (`playing`/`paused`/`stopped`), posição, duração e ID3 |

### Playlist

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/playlist` | Cria/atualiza uma playlist virtual (body = `{ "name": "<nome>", "paths": ["<mp3>", ...] }`) |
| `GET` | `/playlists` | Lista as playlists virtuais existentes |
| `GET` | `/playlist?path=<caminho>` | Escaneia uma pasta e retorna os arquivos `.mp3` (físico) |
| `GET` | `/playlist?path=<arquivo>` | Carrega uma playlist virtual |
| `DELETE` | `/playlist/{name}` | Exclui uma playlist virtual |
| `POST` | `/playlist/rename` | Renomeia uma playlist virtual (body = `{ "oldName": ..., "newName": ... }`) |

### Metadados

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/id3?path=<arquivo>` | Retorna as tags ID3 (artista, título, álbum, etc.) |
| `POST` | `/id3/bulk` | Lista de caminhos → tags ID3 de todos de uma vez |
| `POST` | `/id3/update` | Atualiza tags ID3 de um arquivo |
| `GET` | `/cover?path=<arquivo>` | Capa (jpg/png/webp/gif) da mesma pasta |
| `POST` | `/cover/download` | Baixa a capa do álbum (body = `{ "path": "<mp3>" }`) e salva na pasta do álbum |

### Letras

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/lyrics?path=<arquivo>` | Letra da música (web scraping se não houver cache) |
| `GET` | `/lyrics/cached?path=<arquivo>` | Letra apenas se já houver `.txt` em cache |
| `POST` | `/lyrics` | Salva/edita a letra da música (body = `{ "path": ..., "text": ... }`) |

### Sistema

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/info` | Informações do sistema (arquivo de log, cache, portas) |

## Download de capa do álbum

O endpoint `POST /cover/download` baixa a arte do álbum e salva na pasta do MP3 como `cover.<ext>`:

1. Lê as tags ID3 do arquivo (via `Id3Codec`) e monta o termo de busca "artista + álbum"
2. Busca na **API do iTunes** (`entity=album`, arte em 600×600) — primeira fonte
3. Se não achar, cai para a **API do Deezer** (`cover_xl`, 1000×1000)
4. Baixa a imagem e grava em `cover.jpg`/`png`/`webp`/`gif` na mesma pasta

O `AbstractCoverSearcher` define o Template Method; `ItunesCoverSearcher` e `DeezerCoverSearcher` implementam passos específicos; `CompositeCoverSearcher` orquestra com Strategy.

## Scraper de Letras

O endpoint `/lyrics` usa o scraper para [letras.mus.br](https://www.letras.mus.br):

1. Extrai artista e título das tags ID3 do MP3
2. Constrói slugs e tenta URL direta: `/{artista}/{musica}/`
3. Para bandas com "The", tenta sem o "The" (`the-velvet-underground` → `velvet-underground`)
4. Se falhar, busca em `/?q=<artista>+<musica>` e localiza o link `<a class="gs-title">`
5. Fallbacks: `.gs-title a`, link genérico, match por título
6. Se ainda falhar, tenta a página do artista com o nome invertido (`/mitchell-joni/` para "Joni Mitchell")
7. Remove sufixo `traducao.html` quando presente
8. Extrai `<div class="lyric-original">` e insere `<br>` após cada `<p>`
9. Salva em `{artista} - {musica}.txt` na mesma pasta do MP3

## Repositórios (armazenamento em TXT)

A persistência é feita por abstrações, prontas para escapamento futuro a um banco de dados:

- **Playlists virtuais** → `%USERPROFILE%\.mp3-player\playlists\<nome>.txt`, cada linha um caminho absoluto de MP3
- **Letras** → `<pasta do mp3>\<artista> - <música>.txt`
- **Capas baixadas** → `<pasta do mp3>\cover.<ext>`
- **Cache de metadados** → `~/.mp3-player/metadata-cache.json`

Para migrar a um banco, basta criar novas implementações de `domain/repository/` e declarar como beans.

## CORS

Configurado para permitir origens externas (necessário para o frontend em dev no Vite, porta 8112).

## Como rodar (desenvolvimento)

### Pré-requisitos

- **Java 21**
- **Maven 3.8+**

### Executar

```bash
mvn spring-boot:run
```

A API roda em `http://localhost:8111`.

### Testes

```bash
mvn test
```

### Testes de integração

```bash
mvn test -Dtest=Mp3PlayerApplicationTests
```

Valida que o contexto Spring sobe e injeta todos os beans.

## Como empacotar

### Gerar JAR

```bash
mvn package -DskipTests
```

Gera o fat JAR em `target/mp3-player-<versao>.jar`.

### Gerar JRE mínimo (jlink)

```bash
jlink --module-path $JAVA_HOME/jmods --add-modules java.desktop,java.logging,java.xml,java.net.http,java.sql --output jre --strip-debug --compress zip-6
```

Gera um JRE mínimo (~30MB) contendo apenas os módulos necessários.

### Integração com build do Tauri

O script de release (`scripts/build-release.ps1`) automatiza todo o processo:

1. `mvn package -DskipTests` → gera o fat JAR
2. `jlink` → gera o JRE mínimo
3. Copia JAR e JRE para `frontend/src-tauri/resources/`
4. `npm run build` → compila o Tauri e gera os instaladores

O app desktop embute o backend Java + JRE mínimo, sem exigir Java instalado na máquina de destino.

## Testes

| Teste | Camada | Cobre |
|---|---|---|
| `FilePlaylistRepositoryTest` | infra | persistência TXT ida-e-volta em `@TempDir` |
| `FileLyricRepositoryTest` | infra | gravando e recuperando letra em `.txt` |
| `FileMetadataCacheRepositoryTest` | infra | cache de metadados em JSON |
| `PlayerServiceTest` | application | play/pause/stop/seek e navegação nos 3 modos |
| `PlaylistServiceTest` | application | casos de uso de playlist com repos mockado |
| `LyricsServiceTest` | application | cache hit/miss e scraping com ports mockados |
| `Id3ServiceTest` | application | leitura, bulk e atualização de tags com `Id3Codec` mockado |
| `CoverServiceTest` | application | busca e download de capa com ports mockados |
| `JLayerPlayerEngineTest` | infra | decodificação de MP3 via JLayer |
| `Id3MagicCodecTest` | infra | leitura/escrita de tags ID3 via mp3agic |
| `CachedId3CodecTest` | infra | decorator de cache transparente |
| `FileMusicScannerTest` | infra | escaneamento de pastas |
| `JsoupLyricsScraperTest` | infra | web scraping de letras |
| `ItunesCoverSearcherTest` | infra | busca de capa no iTunes |
| `DeezerCoverSearcherTest` | infra | busca de capa no Deezer |
| `CompositeCoverSearcherTest` | infra | composição de buscadors |
| `PlayerControllerTest` | controller | endpoints de reprodução |
| `PlaylistControllerTest` | controller | endpoints de playlist |
| `MetadataControllerTest` | controller | endpoints de metadados |
| `LyricsControllerTest` | controller | endpoints de letras |
| `InfoControllerTest` | controller | endpoint de info |
| `MusicTest` / `PlaylistTest` | domain | identidade e regras de valor |
| `Mp3PlayerApplicationTests` | integração | contexto Spring carrega e injeta todos os beans |
