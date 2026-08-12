# ovelhafy

Player de música MP3 com frontend desktop (Tauri + React) e backend em Java (Spring Boot).

## Estrutura

```
mp3-player/
├── backend/        # API REST em Java 21 + Spring Boot (Clean Architecture / DDD)
├── frontend/       # App desktop com Tauri + React + Vite + TypeScript
└── README.md       # Este arquivo
```

## Como executar

### Backend

```bash
cd backend
mvn spring-boot:run
```

A API roda em `http://localhost:8111`.

### Frontend (desenvolvimento)

```bash
cd frontend
npm install
npm run dev
```

O Vite abre em `http://localhost:5173`.

### Frontend (build Tauri)

```bash
cd frontend
npm run tauri build
```

### Empacotamento para Windows

Gera um instalador auto-contido (MSI e NSIS) que já embute o backend Java e um JRE mínimo (jlink), sem exigir Java instalado na máquina de destino:

```bash
.\scripts\package-windows.ps1
```

O script compila o JAR do backend, gera o JRE com `jlink`, copia os recursos para `frontend/src-tauri/resources/` e roda `npm run build`. Os instaladores saem em `frontend/src-tauri/target/release/bundle/{msi,nsis}/`. O app inicia e encerra o backend automaticamente em `127.0.0.1:8111`.

## Arquitetura do backend

O backend foi refatorado seguindo **Clean Architecture**, **Clean Code** e **Domain-Driven Design (DDD)**. As dependências apontam sempre para dentro: o domínio não conhece as tecnologias de infraestrutura (JLayer, mp3agic, Jsoup, sistema de arquivos).

```
com.mp3player
├── domain/                 # regras de negócio (núcleo, sem dependências externas)
│   ├── model/              #   entidades: Music, Artist, Album, Playlist, Lyric, Settings, CoverImage
│   ├── port/               #   contratos (interfaces): PlayerEngine, Id3Codec,
│   │                       #                           MusicScanner, LyricsScraper, AlbumCoverSearcher
│   └── repository/         #   portas de persistência: PlaylistRepository, LyricRepository
├── application/            # casos de uso (orquestram os ports, sem infra)
│   ├── player/           #   PlayerService (play, pause, stop, resume, seek, próxima/anterior)
│   ├── playlist/         #   PlaylistAppService (carregar, criar, editar, listar, excluir, renomear, scan de pasta)
│   ├── lyrics/           #   LyricsAppService (buscar letra, web scraping, cache)
│   └── metadata/         #   Id3AppService (ler, bulk, editar tags ID3), CoverAppService (baixar capa)
├── infrastructure/         # implementações concretas dos ports
│   ├── audio/            #   JLayerPlayerEngine (decodificação JLayer)
│   ├── metadata/         #   Id3MagicCodec (mp3agic)
│   ├── music/            #   FileMusicScanner (escaneia pasta)
│   ├── lyrics/           #   JsoupLyricsScraper (letras.mus.br)
│   ├── cover/            #   MusicAlbumCoverSearcher (iTunes + Deezer)
│   └── repository/       #   FilePlaylistRepository, FileLyricRepository
├── web/                  # adaptadores HTTP: PlayerController, PlaylistController,
│                       #                      MetadataController (ID3 + cover), LyricsController
└── config/               # configuração (CORS)
```

A persistência **passa sempre por repositórios** (`PlaylistRepository` e `LyricRepository`). Hoje cada repositório salva em arquivos `.txt`, mas o contrato é independente do armazenamento: para migrar a um banco de dados, basta trocar a implementação em `infrastructure/repository/` sem tocar no domínio nem na aplicação.

## Testes

```bash
cd backend
mvn test
```

O backend cobre com testes unitários (JUnit 5 + Mockito) e um teste de contexto Spring:

- **Repositórios**: `FilePlaylistRepositoryTest`, `FileLyricRepositoryTest` (persistência TXT ida-e-volta em diretório temporário)
- **Casos de uso**: `PlayerServiceTest`, `PlaylistAppServiceTest`, `LyricsAppServiceTest`, `Id3AppServiceTest` (com mocks dos ports/repositórios)
- **Domínio**: `MusicTest`, `PlaylistTest` (identidade e valor)
- **Integração**: `Mp3PlayerApplicationTests` valida a injeção de todos os beans (contexto sobe)

## Funcionalidades principais

- Reprodução de arquivos MP3 (play, pausa, stop, resume, seek)
- Navegação entre faixas (anterior / próxima) com três modos: Contínua, Aleatória e Repetição
- Leitura e edição de tags ID3 (artista, título, álbum, ano, gênero, faixa, duração, bitrate)
- Capa do álbum exibida a partir de arquivos `cover`/`folder`/`album`/`front`/`art`/`artwork` (jpg/png/webp/gif) na pasta da música
- **Download de capa**: clique com o botão direito no placeholder `🎵` abre o menu "Baixar capa do álbum"; o backend busca a capa pelas APIs do iTunes (fallback: Deezer) e salva na pasta do álbum, com atualização automática no Player
- Busca e cache de letras via letras.mus.br com múltiplos fallbacks (URL direta, busca, página do artista com variantes "The", nome invertido e normal)
- **Múltiplas playlists** — além da playlist física (pasta), cria playlists virtuais com o caminho físico de cada música, salvas em `.txt` por um repositório
- Playlist com carregamento, criação, edição, listagem e exclusão (painel de Coleção com duas colunas: todas as músicas × músicas da playlist), cabeçalho com colunas redimensionáveis (Artista | Música | Tempo) e tooltip com os metadados ID3 da faixa
- Gerenciador de coleção: lista de álbuns e artistas de toda a biblioteca com edição das tags ID3 em grade por álbum/artista
- Edição das letras ("Alterar letra") com persistência via `POST /lyrics`
- Barra de progresso clicável (seek)
- Interface escura com tema `#000`/`#0d0d0d`
- Toolbar com navegação entre painéis (letra / coleção / configurações)
- Lógica de negócio reutilizável e testável, independente de frameworks

## Logging

Todos os endpoints e serviços utilizam SLF4J com logs informativos.
