# ovelhafy

<img src="./frontend/icone-v2.png" alt="ícone ovelhafy" width="100">

## Objetivo

Gerenciador e organizador de coleção de músicas local com tocador de mp3. Organizar e alterar as ID3 das músicas, baixar as letras, as capas de álbuns, criar playlists etc.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|---|---|---|
| Runtime backend | Java | 21 |
| Framework backend | Spring Boot | 3.3.5 |
| Build backend | Maven | 3.8+ |
| Decodificador MP3 | JLayer | 1.0.1 |
| Tags ID3 | mp3agic | 0.9.1 |
| Web scraping | Jsoup | 1.18.1 |
| Framework UI | React | 18 |
| Bundler frontend | Vite | 6 |
| Linguagem frontend | TypeScript | strict |
| Desktop | Tauri | v2 |
| Ícones | MUI (Material UI) | 9.2 |
| Testes backend | JUnit 5 + Mockito | — |

---

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Reprodução MP3 | Play, pausa, stop, resume e seek em arquivos MP3 locais |
| Navegação entre faixas | Anterior / próxima com três modos: Contínua, Aleatória e Repetição |
| Auto-play | Reproduz automaticamente a próxima faixa ao término da atual |
| Tags ID3 | Leitura e edição de artista, título, álbum, ano, gênero, faixa, disco, bitrate e duração |
| Edição em lote (bulk) | Edição de tags ID3 de múltiplos arquivos a partir de padrões de nome |
| Capa do álbum | Exibição automática de arquivos de capa (jpg/png/webp/gif) |
| Download de capa | Busca automática via APIs do iTunes (fallback: Deezer) |
| Letras (lyrics) | Busca via web scraping em letras.mus.br com cache local e edição |
| Playlists físicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | Criação, edição (duas colunas), renomeação, exclusão e carregamento |
| Gerenciador de coleção | Lista de álbuns e artistas com edição de tags ID3 em grade |
| Busca avançada | Filtros com operadores lógicos (`&&`, `||`) e filtros por tag |
| Temas | Suporte a tema escuro e claro com CSS custom properties |
| Cache de metadados | Cache em disco usando Decorator Pattern |
| Dicionário | Consulta de palavras em dicionário online (Priberam para português) |

---

## Estrutura do projeto

```
mp3-player/
├── backend/                            # API REST em Java 21 + Spring Boot
│   ├── pom.xml
│   ├── README.md
│   └── src/
│       ├── main/java/com/mp3player/
│       │   ├── Mp3PlayerApplication.java
│       │   ├── config/                 # Wiring de beans (CoverSearcherConfig, Id3CodecConfig)
│       │   ├── shared/                 # Compartilhado (CorsConfig, Settings, MusicFileNaming)
│       │   ├── player/                 # Bounded Context: Reprodução
│       │   │   ├── domain/             #   model (Music), port (PlayerEngine)
│       │   │   ├── application/        #   PlayerService
│       │   │   ├── infrastructure/     #   JLayerPlayerEngine
│       │   │   └── web/                #   PlayerController
│       │   ├── metadata/               # Bounded Context: Tags ID3 + Capas
│       │   │   ├── domain/             #   model (Album, Artist, CoverImage), ports, repository
│       │   │   ├── application/        #   Id3Service, CoverService
│       │   │   ├── infrastructure/     #   Id3MagicCodec, CachedId3Codec, cover searchers
│       │   │   └── web/                #   MetadataController, InfoController
│       │   ├── playlist/               # Bounded Context: Playlists
│       │   │   ├── domain/             #   model (Playlist), port (MusicScanner), repository
│       │   │   ├── application/        #   PlaylistService
│       │   │   ├── infrastructure/     #   FilePlaylistRepository, FileMusicScanner
│       │   │   └── web/                #   PlaylistController
│       │   ├── lyrics/                 # Bounded Context: Letras
│       │   │   ├── domain/             #   model (Lyric), ports, repository
│       │   │   ├── application/        #   LyricsService
│       │   │   ├── infrastructure/     #   CompositeLyricsScraper, FileLyricRepository
│       │   │   └── web/                #   LyricsController
│       │   └── dictionary/             # Bounded Context: Dicionário
│       │       ├── domain/             #   model (DictionaryLookupResult), port
│       │       ├── application/        #   DictionaryLookupService
│       │       ├── infrastructure/     #   PriberamSource
│       │       └── web/                #   DictionaryController
│       ├── main/resources/
│       │   └── application.properties
│       └── test/java/com/mp3player/   # Testes unitários
├── frontend/                           # App desktop com Tauri v2 + React 18
│   ├── package.json
│   ├── README.md
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx                     # Componente raiz (estado global, polling, auto-play)
│   │   ├── App.css                     # Estilos globais + CSS custom properties (temas)
│   │   ├── config.ts
│   │   ├── searchParser.ts
│   │   └── components/
│   │       ├── Player.tsx              # Capa, controles de mídia, barra de progresso
│   │       ├── Playlist.tsx            # Lista de músicas com tooltip ID3
│   │       ├── FolderSelector.tsx      # Input de caminho de pasta
│   │       ├── LyricsPanel.tsx         # Exibição/edição de letras
│   │       ├── CollectionManager.tsx   # Lista de álbuns/artistas com edição em grade
│   │       ├── BulkId3Editor.tsx       # Edição em lote de tags
│   │       ├── InfoModal.tsx           # Dialog "Sobre"
│   │       ├── SettingsPanel.tsx       # Modo de reprodução, tema, capa
│   │       ├── PlaylistManager.tsx     # CRUD de playlists virtuais
│   │       ├── Toolbar.tsx             # Navegação
│   │       └── DictionaryModal.tsx     # Consulta ao dicionário
│   └── src-tauri/                      # Shell Tauri (Rust)
├── scripts/
│   └── build-release.ps1              # Script automatizado de release
├── docs/
│   └── architecture.md                # Diagramas de interação das camadas
└── CHANGELOG.md
```

---

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

---

## Como testar

### Backend

```bash
cd backend
mvn test
```

### Testes de integração

```bash
cd backend
mvn test -Dtest=Mp3PlayerApplicationTests
```

Valida que o contexto Spring sobe e injeta todos os beans.

---

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
6. Empacota: `mvn package -DskipTests` -> `jlink` (JRE mínimo) -> copia JAR para `resources/` -> `npm run build` (Tauri)
7. Cria commit e tag git

O app gerado é auto-contido: embute o backend Java e um JRE mínimo (`jlink`), sem exigir Java instalado na máquina de destino.

---

## Persistência

### Por que não usei banco de dados (por enquanto)

O projeto foi projetado para funcionar como uma aplicação desktop local. Nesta fase, optou-se por não utilizar um banco de dados relacional ou NoSQL para manter a simplicidade de instalação e distribuição. O app é auto-contido e não exige serviços externos. Futuramente, pode-se considerar SQLite ou outro banco embutido para suportar estatísticas de uso (músicas mais tocadas, tempo de reprodução, etc.) e consultas mais complexas.

### O que é salvo em arquivos (backend)

| Dado | Localização | Implementação |
|---|---|---|
| Playlists virtuais | `~/.mp3-player/playlists/<nome>.txt` | `FilePlaylistRepository` — cada playlist é um arquivo TXT com um caminho absoluto por linha |
| Cache de metadados ID3 | `~/.mp3-player/metadata-cache.json` | `FileMetadataCacheRepository` — JSON com todas as tags lidas, mantido em memória e gravado de forma atômica |
| Letras | `<pasta do álbum>/<artista> - <título>.txt` | `FileLyricRepository` — arquivo TXT na mesma pasta dos MP3s, usando tags ID3 para nomear |
| Capas baixadas | `<pasta do álbum>/cover.<ext>` | `CoverService` — imagem salva na pasta do álbum do arquivo MP3 |
| Log do backend | Configurado em `application.properties` | Logback via SLF4J |

### O que é salvo no localStorage (frontend)

| Chave | Tipo | Descrição |
|---|---|---|
| `mp3_folder` | `string` | Caminho da última pasta carregada |
| `mp3_theme` | `'dark' \| 'light'` | Tema selecionado pelo usuário |
