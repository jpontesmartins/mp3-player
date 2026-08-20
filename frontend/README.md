# Frontend — ovelhafy

Aplicação desktop construída com **Tauri v2 + React 18 + Vite + TypeScript (strict)**.

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Framework UI | React 18 |
| Bundler | Vite 6 |
| Linguagem | TypeScript (strict, noUnusedLocals, noUnusedParameters) |
| Desktop | Tauri v2 (Rust) |
| Ícones | MUI (Material UI) 9.2 |
| Estilização | CSS puro |

## Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| Reprodução MP3 | Play, pausa, stop, resume e seek via barra de progresso |
| Navegação entre faixas | Botões anterior/próxima com modos: Contínua, Aleatória e Repetição |
| Auto-play | Reproduz automaticamente a próxima faixa ao término da atual |
| Capa do álbum | Exibe arquivos `cover`/`folder`/`album`/`front`/`art`/`artwork` (jpg/png/webp/gif) |
| Download de capa | Clique direito no placeholder → busca via APIs iTunes/Deezer |
| Letras (lyrics) | Busca via backend, exibição, edição, cache local, controles A+/A- |
| Tags ID3 | Tooltip com metadados, edição inline, edição em grade por álbum/artista |
| Edição em lote | Edição de tags ID3 a partir de padrões de nome do arquivo |
| Playlists físicas | Escaneamento de pastas para arquivos MP3 |
| Playlists virtuais | CRUD completo: criar, editar (duas colunas), renomear, excluir, carregar |
| Busca avançada | Filtros com operadores `&&`, `||` e filtros por tag ID3 |
| Temas | Escuro e claro com CSS custom properties |
| Coleção | Lista de álbuns e artistas com edição de tags |

## Componentes

| Componente | Arquivo | Descrição |
|---|---|---|
| App | `App.tsx` | Componente raiz, gerenciamento de estado, polling, auto-play, cache de ID3 |
| Toolbar | `Toolbar.tsx` | Botões de navegação: Letra, Coleção, Configurações, Info |
| Player | `Player.tsx` | Capa do álbum, controles de mídia, barra de progresso, status, menu de download de capa |
| Playlist | `Playlist.tsx` | Lista de músicas com colunas redimensionáveis, tooltip ID3, busca/filtro |
| LyricsPanel | `LyricsPanel.tsx` | Exibição/edição de letras, controles de tamanho de fonte |
| SettingsPanel | `SettingsPanel.tsx` | Modo de reprodução, tema, exibição de capa, seletor de pasta |
| CollectionManager | `CollectionManager.tsx` | Lista de álbuns/artistas com edição de tags em grade |
| PlaylistManager | `PlaylistManager.tsx` | CRUD de playlists virtuais (duas colunas) |
| BulkId3Editor | `BulkId3Editor.tsx` | Edição em lote de tags a partir de padrões de nome |
| InfoModal | `InfoModal.tsx` | Diálogo "Sobre" com ícone e informações do desenvolvedor |
| FolderSelector | `FolderSelector.tsx` | Input de caminho de pasta + botão carregar |

## Paleta de cores

| Elemento | Cor | Uso |
|---|---|---|
| Fundo da página | `#000` | Body |
| Superfícies | `#0d0d0d` | Painéis (letra, playlist, configurações) |
| Botões padrão | `#1a1a1a` / borda `#333` | Ações genéricas |
| Botões de mídia | `#2a2a2a` / `32×32` | Play/pause/stop/anterior/próxima |
| Texto primário | `#eee` | Títulos, labels |
| Texto secundário | `#888` / `#777` | Status, dicas |
| Link/ativo | `#ccc` / `#eee` | Item selecionado |
| Desabilitado | Opacity `0.4` | Botão inativo |

## Interações

- **Auto-play**: ao fim da música, toca a próxima conforme o modo (Contínua → próxima, Aleatória → aleatória, Repetição → mesma)
- **Intentional stop**: impede auto-play em Stop manual ou carregamento de nova playlist
- **Seek**: clique na barra de progresso para ir a qualquer ponto
- **Capa**: se a imagem falhar (`onError`), exibe placeholder `🎵`; `key` força recriação do `<img>` ao trocar de música
- **Menu de capa**: botão direito no placeholder abre "Baixar capa do álbum"
- **Cache de letras**: arquivos `.txt` salvos na pasta do álbum e reutilizados
- **Polling**: `setInterval` a cada 500ms atualiza barra de progresso e status

## Como rodar (desenvolvimento)

### Pré-requisitos

- **Node.js 18+** e **npm**
- **Rust** (para compilar o Tauri)
- Backend rodando em `http://localhost:8111`

### Instalar dependências

```bash
npm install
```

### Apenas Vite (navegador)

```bash
npm run dev:vite
```

Rota em `http://localhost:8112`.

### Tauri desktop (recomendado)

```bash
npm run dev
```

Inicia o Vite e abre a janela desktop do Tauri. O Rust compila automaticamente na primeira execução.

## Como empacotar

### Build de produção (Tauri)

```bash
npm run build
```

Executa `vite build` (via `beforeBuildCommand`) e compila o binário Tauri + instaladores MSI/NSIS.

Os instaladores ficam em `src-tauri/target/release/bundle/{msi,nsis}/`.

### Pré-requisitos para build

- **Rust toolchain** instalado (via [rustup](https://rustup.rs))
- **Tauri CLI** (incluído nas dependências do projeto)
- **MSVC Build Tools** (Windows) ou equivalentes (Linux/macOS)

## Scripts npm

| Script | Comando | Descrição |
|---|---|---|
| `dev` | `tauri dev` | Modo desenvolvimento desktop (Vite + Tauri) |
| `build` | `tauri build` | Build de produção desktop |
| `dev:vite` | `vite` | Servidor Vite apenas (porta 8112) |
| `build:vite` | `vite build` | Build Vite apenas |
| `preview` | `vite preview` | Preview do build de produção |

## Configuração Tauri

| Configuração | Valor |
|---|---|
| Nome do produto | `ovelhafy` |
| Identificador | `com.mp3player.app` |
| Tamanho da janela | 1400×800 (mínimo: 1000×600) |
| URL de dev | `http://localhost:8112` |
| Frontend dist | `../dist` |
| Bundle targets | `all` (MSI, NSIS, etc.) |

## Estrutura Tauri

```
src-tauri/
├── src/
│   ├── main.rs          # Ponto de entrada
│   └── lib.rs           # Spawn do backend Java como processo filho
├── tauri.conf.json      # Configuração do Tauri
├── Cargo.toml           # Dependências Rust (tauri v2, serde)
├── capabilities/        # Permissões do Tauri
└── icons/               # Ícones da aplicação
```

O `lib.rs` gerencia o ciclo de vida do backend Java: inicia o processo ao abrir a janela e o encerra ao fechar.
