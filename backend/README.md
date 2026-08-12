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

## Arquitetura

O código segue **Clean Architecture** / **Clean Code** / **DDD**. A regra de dependência aponta sempre para dentro: o **domínio** é o núcleo, o **application** orquestra os casos de uso, a **infrastructure** implementa os ports e o **web** expõe os HTTP endpoints. O domínio não toma decisões sobre tecnologia.

```
com.mp3player
├── domain/                 # regras de negócio — sem dependências externas
│   ├── model/              #   entidades: Music, Artist, Album, Playlist, Lyric, Settings, CoverImage
│   ├── port/               #   contratos: PlayerEngine, Id3Codec, MusicScanner, LyricsScraper, AlbumCoverSearcher
│   └── repository/         #   portas de persistência: PlaylistRepository, LyricRepository
├── application/            # casos de uso — orquestram ports e modelos
│   ├── player/           #   PlayerService       (play, pause, stop, resume, seek, próxima/anterior)
│   ├── playlist/         #   PlaylistAppService  (playlists virtuais + físico)
│   ├── lyrics/           #   LyricsAppService    (busca, web scraping, cache)
│   └── metadata/         #   Id3AppService (ler, bulk e atualizar tags), CoverAppService (baixar capa)
├── infrastructure/         # implementações concretas dos ports
│   ├── audio/            #   JLayerPlayerEngine
│   ├── metadata/         #   Id3MagicCodec (mp3agic)
│   ├── music/            #   FileMusicScanner
│   ├── lyrics/           #   JsoupLyricsScraper
│   ├── cover/            #   MusicAlbumCoverSearcher (iTunes + Deezer)
│   └── repository/       #   FilePlaylistRepository, FileLyricRepository
├── web/                    # adaptadores HTTP: PlayerController, PlaylistController,
│                           #                     MetadataController (ID3 + cover), LyricsController
└── config/                 # CORS
```

> O diagrama acima usa indentação para indicar o pacote-pai; por exemplo, `PlayerService` está em `application/player/`.

O pacote `web/` foi dividido em **Controllers por módulo**, cada um delegando ao service correspondente:

| Controller | Rotas |
|---|---|
| `PlayerController` | `/play`, `/pause`, `/stop`, `/seek`, `/resume`, `/playing` |
| `PlaylistController` | `/playlist`, `/playlists`, `/playlist/{name}`, `/playlist/rename` |
| `MetadataController` | `/id3`, `/id3/bulk`, `/id3/update`, `/cover`, `/cover/download` |
| `LyricsController` | `/lyrics`, `/lyrics/cached`, `/lyrics` (POST) |

### Padrões aplicados

- **Ports & Adapters (Hexagonal)**: o domínio define interfaces (`PlayerEngine`, `Id3Codec`, `MusicScanner`, `LyricsScraper`, `PlaylistRepository`, `LyricRepository`) e a infraestrutura fornece implementações. Trocar de biblioteca (ex.: mp3agic → outra) ou de armazenamento (TXT → banco) não toca o núcleo.
- **Entities de domínio**: `Music`, `Artist`, `Album`, `Playlist`, `Lyric`, `Settings` encapsulam invariantes próprias.
- **Repositorios como abstração de persistência**: `FilePlaylistRepository` grava playlists virtuais (`%USERPROFILE%\.mp3-player\playlists\*.txt`) e `FileLyricRepository` grava letras (`{artista} - {música}.txt` ao lado do MP3). A infra decide o formato; o contrato é independente.
- **Injeção de dependência**: o Spring injeta os adapters nos services via construtor.
- **Logging**: SLF4J em todos os services e endpoints.

## Testes

```bash
mvn test
```

| Teste | Camada | Cobre |
|---|---|---|
| `FilePlaylistRepositoryTest` | infra | persistência TXT ida-e-volta (criar, carregar, listar, renomear, excluir) em `@TempDir` |
| `FileLyricRepositoryTest` | infra | gravando e recuperando letra em `.txt` |
| `PlayerServiceTest` | application | play/pause/stop/seek e navegação (próxima/anterior) nos modos contínuo, aleatório e repetição |
| `PlaylistAppServiceTest` | application | casos de uso de playlist com repos mockado |
| `LyricsAppServiceTest` | application | cache hit/miss e scraping com ports mockados |
| `Id3AppServiceTest` | application | leitura, bulk e atualização de tags com `Id3Codec` mockado |
| `MusicTest` / `PlaylistTest` | domain | identidade e regras de valor |
| `Mp3PlayerApplicationTests` | integração | contexto Spring carrega e injeta todos os beans |

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
| `GET` | `/playlist?path=<caminho>` | Carrega uma playlist (`path` = arquivo `.txt` da playlist virtual) |
| `GET` | `/playlist?path=<pasta>` | Escaneia uma pasta e retorna os arquivos `.mp3` (físico) |
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
| `GET` | `/lyrics?path=<arquivo>` | Letra da música (web scraping se não houver cache) |
| `GET` | `/lyrics/cached?path=<arquivo>` | Letra apenas se já houver `.txt` em cache |
| `POST` | `/lyrics` | Salva/edita a letra da música (body = `{ "path": ..., "text": ... }`) |

## Download de capa do álbum

O endpoint `POST /cover/download` baixa a arte do álbum e salva na pasta do MP3 como `cover.<ext>`:

1. Lê as tags ID3 do arquivo (via `Id3Codec`) e monta o termo de busca "artista + álbum" (deduplica quando o álbum repete o artista).
2. Busca a capa na **API de busca do iTunes** (`entity=album`, arte em 600×600) — primeira fonte.
3. Se não achar, cai para a **API do Deezer** (`cover_xl`, 1000×1000).
4. Baixa a imagem original e grava em `cover.jpg`/`png`/`webp`/`gif` na mesma pasta, pronta para ser servida por `GET /cover`.

> O scrap de imagens do Google/Bing foi descartado: o Google só entrega uma página shell que exige JavaScript, e o Bing retornava resultados irrelevantes para as buscas. O `AlbumCoverSearcher` (port) deixa a fonte plugável.

## Scraper de Letras

O endpoint `/lyrics` usa o scraper para [letras.mus.br](https://www.letras.mus.br) (implementado em `infrastructure/lyrics/JsoupLyricsScraper`):

1. Extrai artista e título das tags ID3 do MP3
2. Constrói slugs e tenta URL direta: `/{artista}/{musica}/` — para bandas com "The", tenta também sem o "The" (`the-velvet-underground` e `velvet-underground`)
3. Se falhar, busca em `/?q=<artista>+<musica>` e localiza o link `<a class="gs-title">`
4. Fallbacks: `.gs-title a`, link genérico, match por título
5. Se ainda falhar, tenta a página do artista com o nome invertido (`/mitchell-joni/` para "Joni Mitchell"), primeiro via URL direta da música e depois procurando o link pelo título na listagem; tenta as combinações invertido/normal × com/sem "The" (páginas inexistentes são ignoradas com `ignoreHttpErrors`)
6. Remove sufixo `traducao.html` quando presente
7. Extrai `<div class="lyric-original">` e insere `<br>` após cada `<p>`
8. Salva em `{artista} - {musica}.txt` na mesma pasta do MP3 (via `LyricRepository`)

## Repositórios (armazenamento em TXT)

A persistência é feita por abstrações, prontas para escapamento futuro a um banco de dados:

- **Playlists virtuais** → `%USERPROFILE%\.mp3-player\playlists\<nome>.txt`, cada linha um caminho absoluto de MP3.
- **Letras** → `<pasta do mp3>\<artista> - <música>.txt`.
- **Capas baixadas** → `<pasta do mp3>\cover.<ext>` (gera provider via `GET /cover`).

Para migrar a um banco, basta criar novas implementações de `domain/repository/PlaylistRepository` e `LyricRepository` e declarar como beans; o `application/` e o `web/` não mudam.

## CORS

Configurado para permitir origens externas (necessário para o frontend em dev no Vite).
