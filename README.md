# ovelhafy

Player de música MP3 com frontend desktop (Tauri + React) e backend em Java (Spring Boot).

## Estrutura

```
mp3-player/
├── backend/        # API REST em Java 21 + Spring Boot (Clean Architecture / DDD)
├── frontend/       # App desktop com Tauri v2 + React 18 + Vite + TypeScript
├── scripts/        # Scripts de build e release
```

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Reprodução MP3 | Play, pausa, stop, resume e seek em arquivos MP3 locais |
| Navegação entre faixas | Anterior / próxima com três modos: Contínua, Aleatória e Repetição |
| Auto-play | Reproduz automaticamente a próxima faixa ao término da atual, conforme o modo selecionado |
| Tags ID3 | Leitura e edição de artista, título, álbum, ano, gênero, faixa, disco, bitrate e duração |
| Edição em lote (bulk) | Edição de tags ID3 de múltiplos arquivos a partir de padrões de nome do arquivo |
| Capa do álbum | Exibição automática de arquivos `cover`/`folder`/`album`/`front`/`art`/`artwork` (jpg/png/webp/gif) |
| Download de capa | Busca automática via APIs do iTunes (fallback: Deezer) com clique direito no placeholder |
| Letras (lyrics) | Busca via web scraping em letras.mus.br com cache local, edição e controle de tamanho da fonte |
| Playlists físicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | Criação, edição (duas colunas: todas as músicas × playlist), renomeação, exclusão e carregamento |
| Gerenciador de coleção | Lista de álbuns e artistas com edição de tags ID3 em grade |
| Busca avançada | Filtros com operadores lógicos (`&&`, `||`) e filtros por tag (`<artist> == Nirvana`, `<year> > 1990`) |
| Temas | Suporte a tema escuro e claro com CSS custom properties |
| Cache de metadados | Cache em disco (`~/.mp3-player/metadata-cache.json`) usando Decorator Pattern |

## Como rodar (desenvolvimento)

### Pré-requisitos

- **Java 21**
- **Maven 3.8+**
- **Node.js 18+** e **npm**
- **Rust** (para compilar o Tauri)

### Backend

```bash
cd backend
mvn spring-boot:run
```

A API roda em `http://localhost:8111`.

### Frontend (apenas Vite)

```bash
cd frontend
npm install
npm run dev:vite
```

O Vite roda em `http://localhost:8112`.

### Frontend (Tauri desktop)

```bash
cd frontend
npm install
npm run dev
```

Inicia o Vite e abre a janela desktop do Tauri apontando para `http://localhost:8112`.

### Testes do backend

```bash
cd backend
mvn test
```

## Como empacotar

### Build rápido (apenas frontend)

```bash
cd frontend
npm run build
```

Gera o binário Tauri + instaladores MSI/NSIS em `frontend/src-tauri/target/release/bundle/`.

### Release completa (automatizada)

O script `scripts/build-release.ps1` faz tudo automaticamente: versionamento semântico, changelog, compilação do JAR, geração do JRE mínimo via `jlink`, e empacotamento Tauri:

```powershell
# Do diretório raiz do projeto:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1

# Opções:
#   -Minor              # Bump de versão minor
#   -Major              # Bump de versão major
#   -Version "X.Y.Z"   # Versão explícita
#   -DryRun             # Simula sem alterar nada
```

O que o script faz:
1. Lê a última tag git
2. Coleta e classifica commits desde a última tag
3. Calcula a nova versão semântica
4. Atualiza versão em `package.json`, `tauri.conf.json`, `Cargo.toml` e `App.tsx`
5. Gera e insere entrada no `CHANGELOG.md`
6. Empacota: `mvn package -DskipTests` → `jlink` (JRE mínimo) → copia JAR para `resources/` → `npm run build` (Tauri)
7. Cria commit e tag git

O app gerado é auto-contido: embute o backend Java e um JRE mínimo (`jlink`), sem exigir Java instalado na máquina de destino.

## Arquitetura do backend

O backend segue **Clean Architecture**, **Clean Code** e **Domain-Driven Design (DDD)**. As dependências apontam sempre para dentro: o domínio não conhece as tecnologias de infraestrutura (JLayer, mp3agic, Jsoup, sistema de arquivos).

```
com.mp3player
├── domain/                 # regras de negócio (núcleo, sem dependências externas)
│   ├── model/              #   entidades: Music, Artist, Album, Playlist, Lyric, Settings, CoverImage
│   ├── port/               #   contratos (interfaces): PlayerEngine, Id3Codec,
│   │                       #                           MusicScanner, LyricsScraper, AlbumCoverSearcher
│   └── repository/         #   portas de persistência: PlaylistRepository, LyricRepository, MetadataCacheRepository
├── application/            # casos de uso (orquestram os ports, sem infra)
│   ├── player/             #   PlayerService (play, pause, stop, resume, seek, próxima/anterior)
│   ├── playlist/           #   PlaylistService (carregar, criar, editar, listar, excluir, renomear, scan de pasta)
│   ├── lyrics/             #   LyricsService (buscar letra, web scraping, cache)
│   └── metadata/           #   Id3Service (ler, bulk, editar tags ID3), CoverService (baixar capa)
├── infrastructure/         # implementações concretas dos ports
│   ├── audio/              #   JLayerPlayerEngine (decodificação JLayer)
│   ├── metadata/           #   Id3MagicCodec (mp3agic), CachedId3Codec (Decorator)
│   ├── music/              #   FileMusicScanner (escaneia pasta)
│   ├── lyrics/             #   JsoupLyricsScraper (letras.mus.br)
│   ├── cover/              #   AbstractCoverSearcher, ItunesCoverSearcher, DeezerCoverSearcher, CompositeCoverSearcher, CoverDownloader
│   └── repository/         #   FilePlaylistRepository, FileLyricRepository, FileMetadataCacheRepository
├── controller/             # adaptadores HTTP: PlayerController, PlaylistController,
│                           #                     MetadataController, LyricsController, InfoController
└── config/                 # configuração (CORS, beans, propriedades)
```

A persistência **passa sempre por repositórios** (`PlaylistRepository`, `LyricRepository` e `MetadataCacheRepository`). Hoje cada repositório salva em arquivos `.txt`/`.json`, mas o contrato é independente do armazenamento.

### Padrões de design aplicados

| Padrão | Onde | O que faz |
|---|---|---|
| Decorator | `CachedId3Codec` | Envolv `Id3MagicCodec` com cache transparente |
| Template Method | `AbstractCoverSearcher` | Define fluxo de busca; subclasses implementam passos específicos |
| Strategy / Composite | `CompositeCoverSearcher` | Encadeia múltiplos buscadors (iTunes → Deezer fallback) |
| Ports & Adapters | Domain defines interfaces | Infrastructure fornece implementações |
| Repository | `PlaylistRepository`, etc. | Abstrai persistência, facilmente trocável |

## Cache de metadados ID3

Ler tags ID3 de cada arquivo MP3 é uma operação custosa. Para evitar reler os mesmos arquivos toda vez, a aplicação mantém um **cache em disco** (`~/.mp3-player/metadata-cache.json`).

```
1. CachedId3Codec.read(caminho)
2. → Verifica MetadataCacheRepository.get(caminho)
3.   → Hit:  retorna Music a partir do cache (sem ler arquivo)
4.   → Miss: delega para Id3MagicCodec.read(caminho)
5.          → Armazena resultado no cache via MetadataCacheRepository.put()
6.          → Retorna Music
```

## Logging

Todos os endpoints e serviços utilizam SLF4J com logs informativos.
